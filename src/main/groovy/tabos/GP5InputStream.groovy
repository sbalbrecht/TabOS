package tabos

import java.nio.ByteBuffer
import java.nio.ByteOrder

class GP5InputStream extends FilterInputStream {
    static final int BEND_POSITION = 60
    static final int BEND_SEMITONE = 25
    static final VERSIONS = [
        'FICHIER GUITAR PRO v5.00': v(5, 0, 0),
        'FICHIER GUITAR PRO v5.10': v(5, 1, 0),
    ]

    GP5InputStream(InputStream stream) { super(stream) }

    Song readSong() {
        final Song song = new Song()

        song.version = readFixedLengthStringField 30
        final Tuple version = VERSIONS[song.version]

        // todo if isClipboard copyClipboard?
        song.title = readFixedLengthStringField()
        song.subtitle = readFixedLengthStringField()
        song.artist = readFixedLengthStringField()
        song.album = readFixedLengthStringField()
        song.wordsAuthor = readFixedLengthStringField()
        song.musicAuthor = readFixedLengthStringField()
        song.copyright = readFixedLengthStringField()
        song.tabAuthor = readFixedLengthStringField()
        song.instructions = readFixedLengthStringField()
        song.notice = (0..<readInt()).collect { readFixedLengthStringField() }
        song.lyrics = new Lyrics(
            lyricTrackIndex: readInt(),
            lines: (0..<5).collect { new LyricLine(
                startingMeasure: readInt(),
                line: readVariableLengthStringField()
            )}
        )

        song.rseMasterEffect = version > v(5, 0, 0) ? new RSEMasterEffect(
            volume: readInt().tap {
                readInt() // unknown
            },
            equalizer: new RSEEqualizer((0..<11).collect{ (-read() / 10) as float })
        ) : null

        song.pageSetup = new PageSetup().tap {
            dimensions.width = readInt()
            dimensions.height = readInt()
            margin.left = readInt()
            margin.right = readInt()
            margin.top = readInt()
            margin.bottom = readInt()
            scoreSizeProportion = readInt() / 100

            short templateFlags = readShort()
            title = new HeaderElement(readFixedLengthStringField(), bool(templateFlags & 0x001))
            subtitle = new HeaderElement(readFixedLengthStringField(), bool(templateFlags & 0x002))
            artist = new HeaderElement(readFixedLengthStringField(), bool(templateFlags & 0x004))
            album = new HeaderElement(readFixedLengthStringField(), bool(templateFlags & 0x008))
            words = new HeaderElement(readFixedLengthStringField(), bool(templateFlags & 0x010))
            music = new HeaderElement(readFixedLengthStringField(), bool(templateFlags & 0x020))
            wordsAndMusic = new HeaderElement(readFixedLengthStringField(), bool(templateFlags & 0x040))
            copyright1 = new HeaderElement(readFixedLengthStringField(), bool(templateFlags & 0x080))
            copyright2 = new HeaderElement(readFixedLengthStringField(), bool(templateFlags & 0x080))
            pageNumber = new HeaderElement(readFixedLengthStringField(), bool(templateFlags & 0x100))
        }
        song.tempoName = readFixedLengthStringField()
        song.tempo = readInt()
        song.hideTempo = version > v(5, 0, 0) ? readBoolean() : false
        song.key = KeySignature.from(read(), 0)

        readInt() // unused "octave"?

        List<MidiChannel> midiChannels = (0..<64).collect { i ->
            def toChannelShort = { int data -> Math.min(Math.max((data << 3) - 1, -1), 32767) + 1 }
            new MidiChannel().tap {
                channel = i
                effectChannel = i
                instrument = readInt().with {
                    it == -1 && channel == DEFAULT_PERCUSSION_CHANNEL ? 0 : it
                }
                volume = toChannelShort(read())
                balance = toChannelShort(read())
                chorus = toChannelShort(read())
                reverb = toChannelShort(read())
                phaser = toChannelShort(read())
                tremolo = toChannelShort(read())
                bank = i == DEFAULT_PERCUSSION_CHANNEL ? 128 : 0
                skipBytes 2 // gp3 compatibility
            }
        }

        song.directions = [
            signs: [
                'Coda': readShort(),
                'Double Coda': readShort(),
                'Segno': readShort(),
                'Segno Segno': readShort(),
                'Fine': readShort()
            ],
            fromSigns: [
                'da Capo': readShort(),
                'da Capo al Coda': readShort(),
                'da Capo al Double Coda': readShort(),
                'da Capo al Fine': readShort(),
                'da Segno': readShort(),
                'da Segno al Coda': readShort(),
                'da Segno al Double Coda': readShort(),
                'da Segno al Fine': readShort(),
                'da Segno Segno': readShort(),
                'da Segno Segno al Coda': readShort(),
                'da Segno Segno al Double Coda': readShort(),
                'da Segno Segno al Fine': readShort(),
                'da Coda': readShort(),
                'da Double Coda': readShort()
            ]
        ]

        final Map signsByMeasure = song.directions.signs.groupBy { it.value }
        final Map fromSignsByMeasure = song.directions.fromSigns.groupBy { it.value }

        int rseMasterEffectReverb = readInt()
        song.rseMasterEffect?.reverb = rseMasterEffectReverb

        final int numMeasures = readInt()
        final int numTracks = readInt()

        (0..<numMeasures).each { i ->
            if (i > 0) skipBytes 1
            song.addMeasureHeader new MeasureHeader().tap { measureHeader ->
                MeasureHeader prevHeader = (i == 0) ? null : song.measureHeaders[i - 1]
                measureHeader.song = song
                final int flags = readUnsignedByte()
                number = i + 1
                isRepeatOpen = bool(flags & 0x04)
                hasDoubleBar = bool(flags & 0x80)
                timeSignature = new TimeSignature(
                    numerator: (bool(flags & 0x01)) ? read() : prevHeader.timeSignature.numerator,
                    denominator: (bool(flags & 0x02)) ? new Duration(read()) : prevHeader.timeSignature.denominator.clone()
                )
                repeatClose = (bool(flags & 0x08) ? read() : -1).with { it > -1 ? it - 1 : it}
                marker = bool(flags & 0x20) ? new Marker(
                    title: readFixedLengthStringField(),
                    color: new Color(
                        r: readUnsignedByte(),
                        g: readUnsignedByte(),
                        b: readUnsignedByte()
                    ).tap { skipBytes 1 }
                ) : null
                keySignature = (bool(flags & 0x40)) ? (KeySignature.from(read(), read()) ?: prevHeader.keySignature) : prevHeader.keySignature
                repeatAlternative = bool(flags & 0x10) ? read() : 0
                timeSignature.beams = bool(flags & 0x03) ? (0..<4).collect { read() } : prevHeader.timeSignature.beams
                if ((flags & 0x10) == 0) skipBytes 1 // Always 0
                tripletFeel = TripletFeel.from(read())

                // gp5
                direction = signsByMeasure[i] ?: null
                fromDirection = fromSignsByMeasure[i] ?: null

                // todo verify working
                start = (i == 0) ? Duration.QUARTER_TIME : prevHeader.start + measureHeader.length()
            }
        }

        song.tracks = (0..<numTracks).collect { trackIdx ->
            if (trackIdx == 0 || version == v(5, 0, 0)) skipBytes(1) // Always 0
            readUnsignedByte().with { int flags -> new Track().tap { track ->
                track.song = song
                number = trackIdx + 1
                isPercussionTrack = bool(flags & 0x01)
                is12StringedGuitarTrack = bool(flags & 0x02)
                isBanjoTrack = bool(flags & 0x04)
                isVisible = bool(flags & 0x08)
                isSolo = bool(flags & 0x10)
                isMute = bool(flags & 0x20)
                useRSE = bool(flags & 0x40)
                indicateTuning = bool(flags & 0x80)
                name = readFixedLengthStringField 40
                strings = readInt().with { stringCount ->
                    (0..<7).collect {
                        readInt()
                    }.indexed().collect { i, tuning -> new GuitarString(
                        number: i + 1,
                        value: tuning
                    )}[0..<stringCount]
                }
                port = readInt()
                final int index = readInt() - 1
                final int effectChannel = readInt() - 1
                if (index in (0..<midiChannels.size())) {
                    MidiChannel trackChannel = midiChannels[index]
                    if (trackChannel.instrument < 0)
                        trackChannel.instrument = 0
                    if (trackChannel.channel != MidiChannel.DEFAULT_PERCUSSION_CHANNEL)
                        trackChannel.effectChannel = effectChannel
                    channel = trackChannel
                }
                fretCount = readInt()
                offset = readInt()
                color = new Color(
                    r: readUnsignedByte(),
                    g: readUnsignedByte(),
                    b: readUnsignedByte()
                ).tap { skipBytes 1 }
                settings = readShort().with { short settingsFlags -> new TrackSettings(
                    tablature: bool(settingsFlags & 0x001),
                    notation: bool(settingsFlags & 0x002),
                    diagramsAreBelow: bool(settingsFlags & 0x004),
                    showRhythm: bool(settingsFlags & 0x008),
                    forceHorizontal: bool(settingsFlags & 0x010),
                    forceChannels: bool(settingsFlags & 0x020),
                    diagramList: bool(settingsFlags & 0x040),
                    diagramsInScore: bool(settingsFlags & 0x080),
                    unknown: bool(settingsFlags & 0x100), // fixme 0x0100 ?
                    autoLetRing: bool(settingsFlags & 0x200),
                    autoBrush: bool(settingsFlags & 0x400),
                    extendRhythmic: bool(settingsFlags & 0x800),
                )}
                rse = new TrackRSE()
                rse.autoAccentuation = Accentuation.from(read())
                channel.bank = read()
                rse.humanize = read().tap {
                    readInt() // ?
                    readInt() // ?
                    readInt() // ?
                    skipBytes 12 // ?
                }
                rse.instrument = new RSEInstrument(
                    instrument: readInt(),
                    unknown: readInt(), // fixme ? mostly 1
                    soundBank: readInt(),
                    effectNumber: (version > v(5, 0, 0)) ? readInt() : readShort().tap { skip 1 }
                )
                if (version > v(5, 0, 0)) {
                    rse.equalizer = new RSEEqualizer((0..<4).collect{ (-read() / 10) as float })
                    rse.instrument.effect = readFixedLengthStringField()
                    rse.instrument.effectCategory = readFixedLengthStringField()
                }
                track
            }}
        }

        skipBytes(version > v(5, 0, 0) ? 1 : 2)

        // read measures
        song.measureHeaders.each { header ->
            song.tracks.each { track ->
                track.measures << new Measure(track, header).tap { measure ->
                    voices = (0..<MAX_VOICES).collect { voiceIdx -> new Voice(measure).tap { voice ->
                        beats = (0..readInt()).collect { beatIdx -> new Beat(voice).tap { beat ->
                            final int beatFlags = readUnsignedByte()
                            status = BeatStatus.from(bool(beatFlags & 0x40) ? read() : BeatStatus.NORMAL.value)
                            duration = new Duration(
                                value: 1 << (read() + 2),
                                isDotted: bool(beatFlags & 0x01),
                                tuplet: bool(beatFlags & 0x20) ? readInt().with { int enters -> new Tuplet(
                                    enters: enters,
                                    times: highestOneBit(enters)
                                )} : null
                            )
                            effect.chord = bool(beatFlags & 0x02) ? new Chord().tap {
                                newFormat = readBoolean()
                                if (newFormat) {
                                    sharp = readBoolean()
                                    skipBytes 3
                                    final def intonation = it.sharp ? Pitch.Intonation.SHARP : Pitch.Intonation.FLAT
                                    root = new Pitch(read(), intonation)
                                    type = ChordType.from(read())
                                    extension = ChordExtension.from(read())
                                    bass = new Pitch(readInt(), intonation)
                                    tonality = ChordAlteration.from(readInt())
                                    add = readBoolean()
                                    name = readFixedLengthStringField 22
                                    fifth = ChordAlteration.from(read())
                                    ninth = ChordAlteration.from(read())
                                    eleventh = ChordAlteration.from(read())
                                    firstFret = readInt()
                                    strings = (0..<7).collect { i -> new GuitarString(i + 1, readInt()) }[0..<track.strings.size()]
                                    barres = read().with { barresCount ->
                                        [
                                            (0..<5).collect { read() },
                                            (0..<5).collect { read() },
                                            (0..<5).collect { read() },
                                        ].transpose()[0..<barresCount].collect(Barre::new) as List<Barre>
                                    }
                                    omissions = (0..<7).collect { readBoolean() }
                                    skipBytes 1
                                    fingerings = (0..<7).collect { Fingering.from(read()) }
                                    show = readBoolean()
                                } else {
                                    name = readFixedLengthStringField()
                                    firstFret = readInt()
                                    strings = firstFret ? (0..<7).collect { i -> new GuitarString(i + 1, readInt()) }[0..<track.strings.size()] : [new GuitarString(-1, -1)] * track.strings.size()
                                }
                            } : null
                            println "measure=$header.number, track=$track.number voice=$voiceIdx beat=$beatIdx"
                            text = bool(beatFlags & 0x04) ? readFixedLengthStringField() : null
                            // beat effects
                            if (bool(beatFlags & 0x08)) {
                                short beatEffectFlags = readShort()
                                effect.vibrato = bool(beatEffectFlags & 0x02)
                                effect.fadeIn = bool(beatEffectFlags & 0x10)
                                if (bool(beatEffectFlags & 0x20)) {
                                    effect.slapEffect = SlapEffect.from(read())
                                }
                                if (bool(beatEffectFlags & 0x400)) {
                                    effect.tremoloBar = readInt().with { bendValue ->
                                        effect.slapEffect ? null : new BendEffect(
                                            value: bendValue,
                                            type: BendType.DIP,
                                            points: [
                                                new BendPoint(0, 0),
                                                new BendPoint(
                                                    Math.round(BendEffect.MAX_POSITION / 2) as int,
                                                    Math.round(-bendValue / 25) as int
                                                ),
                                                new BendPoint(BendEffect.MAX_POSITION, 0)
                                            ]
                                        )
                                    }
                                }
                                if (bool(beatEffectFlags & 0x40)) {
                                    effect.stroke = [read(), read()].with { strokeUp, strokeDown ->
                                        if (strokeUp > 0) new BeatStroke(
                                            direction: BeatStrokeDirection.DOWN, // swapped
                                            value: strokeUp // fixme map to duration
                                        ) else if (strokeDown > 0) new BeatStroke(
                                            direction: BeatStrokeDirection.UP, // swapped
                                            value: strokeDown // fixme map to duration
                                        ) else null
                                    }
                                }
                                effect.hasRasgueado = bool(beatEffectFlags & 0x100)
                                effect.pickStroke = bool(beatEffectFlags & 0x200) ? BeatStrokeDirection.from(read()) : BeatStrokeDirection.NONE
                            }
                            beat.effect.mixTableChange = bool(beatFlags & 0x10) ? new MixTableChange().tap {
                                Closure<MixTableItem> toMixTableItem = { int value -> value >= 0 ? new MixTableItem(value) : null }
                                instrument = toMixTableItem(read())
                                rse = new RSEInstrument(
                                    instrument: readInt(),
                                    unknown: readInt(), // fixme ? mostly 1
                                    soundBank: readInt(),
                                    effectNumber: (version == v(5, 0, 0)) ? readShort().tap { skip 1 } : readInt(),
                                ).tap { if (version == v(5, 0, 0)) skipBytes 1 }

                                volume = toMixTableItem(read())
                                balance = toMixTableItem(read())
                                chorus = toMixTableItem(read())
                                reverb = toMixTableItem(read())
                                phaser = toMixTableItem(read())
                                tremolo = toMixTableItem(read())
                                tempoName = readFixedLengthStringField() // gp5
                                tempo = toMixTableItem(readInt())

                                volume?.duration = read()
                                balance?.duration = read()
                                chorus?.duration = read()
                                reverb?.duration = read()
                                phaser?.duration = read()
                                tremolo?.duration = read()
                                tempo?.duration = read()
                                hideTempo = !tempo && version > v(5, 0, 0) && readBoolean()

                                // gp4 additions
                                int mixTableChangeFlags = readUnsignedByte()
                                volume?.allTracks = bool(mixTableChangeFlags & 0x01)
                                balance?.allTracks = bool(mixTableChangeFlags & 0x02)
                                chorus?.allTracks = bool(mixTableChangeFlags & 0x04)
                                reverb?.allTracks = bool(mixTableChangeFlags & 0x08)
                                phaser?.allTracks = bool(mixTableChangeFlags & 0x10)
                                tremolo?.allTracks = bool(mixTableChangeFlags & 0x20)

                                // gp5 additions
                                useRSE = bool(mixTableChangeFlags & 0x40)
                                wah = new WahEffect(
                                    value: read(),
                                    display: bool(mixTableChangeFlags & 0x80)
                                )
                                if (instrument < 0) rse = null
                                // read rse effect
                                if (version > v(5, 0, 0)) {
                                    String effect = readFixedLengthStringField()
                                    String effectCategory = readFixedLengthStringField()
                                    if (rse) {
                                        rse.effect = effect
                                        rse.effectCategory = effectCategory
                                    }
                                }
                                it
                            } : null

                            // read notes
                            final int stringFlags = readUnsignedByte()
                            notes = track.strings.findAll {
                                stringFlags & 1 << (7 - it.number())
                            }.collect { string -> new Note().tap { Note note ->
                                int noteFlags = readUnsignedByte()
                                note.beat = beat
                                note.string = string.number()
                                effect = new NoteEffect()
                                effect.heavyAccentuatedNote = bool(noteFlags & 0x02)
                                effect.ghostNote = bool(noteFlags & 0x04)
                                effect.accentuatedNote = bool(noteFlags & 0x40)
                                type = bool(noteFlags & 0x20) ? NoteType.from(read()) : NoteType.NORMAL
                                velocity = bool(noteFlags & 0x10) ? unpackVelocity(read()) : Velocities.defaultVelocity
                                value = bool(noteFlags & 0x20) ? readNoteValue(note) : 0
                                effect.leftHandFinger = bool(noteFlags & 0x80) ? Fingering.from(read()) : null
                                effect.rightHandFinger = bool(noteFlags & 0x80) ? Fingering.from(read()) : null
                                durationPercent = bool(noteFlags & 0x01) ? readDouble() : 1.0
                                swapAccidentals = bool(read() & 0x02)
                                if (bool(noteFlags & 0x08)) {
                                    short noteEffectFlags = readShort()
                                    effect.hammer = bool(noteEffectFlags & 0x0002)
                                    effect.letRing = bool(noteEffectFlags & 0x0008)
                                    effect.staccato = bool(noteEffectFlags & 0x0100)
                                    effect.palmMute = bool(noteEffectFlags & 0x0200)
                                    effect.vibrato = bool(noteEffectFlags & 0x4000)
                                    effect.bend = bool(noteEffectFlags & 0x0001) ? new BendEffect(
                                        type: BendType.from(read()),
                                        value: readInt(),
                                        points: (0..readInt()).collect { new BendPoint(
                                            position: Math.round(readInt() * BendEffect.MAX_POSITION / BEND_POSITION),
                                            value: Math.round(readInt() * BendEffect.SEMITONE_LENGTH / BEND_SEMITONE),
                                            vibrato: readBoolean(),
                                        )}
                                    ).with { bend -> bend.points ? bend : null } : null
                                    effect.grace = bool(noteEffectFlags & 0x0010) ? new GraceEffect().tap {
                                        fret = read()
                                        velocity = unpackVelocity(read())
                                        transition = GraceEffectTransition.from(read())
                                        duration = 1 << (7 - read())
                                        int graceFlags = readUnsignedByte()
                                        isDead = (graceFlags & 0x01) != 0
                                        isOnBeat = (graceFlags & 0x02) != 0
                                    } : null
                                    effect.tremoloPicking = bool(noteEffectFlags & 0x0400) ? new TremoloPickingEffect(
                                        duration: new Duration(
                                            value: switch (read()) {
                                                case 1 -> Duration.EIGHTH
                                                case 2 -> Duration.SIXTEENTH
                                                case 3 -> Duration.THIRTY_SECOND
                                                default -> throw new RuntimeException("Invalid tremolo picking effect duration $it")
                                            }
                                        )
                                    ) : null
                                    effect.slides = bool(noteEffectFlags & 0x0800) ? readUnsignedByte().with { slideFlags ->
                                        [
                                            0x01: SlideType.SHIFT_SLIDE_TO,
                                            0x02: SlideType.LEGATO_SLIDE_TO,
                                            0x04: SlideType.OUT_DOWNWARDS,
                                            0x08: SlideType.OUT_UPWARDS,
                                            0x10: SlideType.INTO_FROM_BELOW,
                                            0x20: SlideType.INTO_FROM_ABOVE
                                        ].findAll { mask, ignored -> bool(slideFlags & mask) }.values()
                                    } : []
                                    effect.harmonic = bool(noteEffectFlags & 0x1000) ? switch (read()) {
                                        case 1 -> new NaturalHarmonic()
                                        case 2 -> new ArtificialHarmonic(
                                            pitch: new Pitch(read(), read()),
                                            octave: Octave.from(read())
                                        )
                                        case 3 -> new TappedHarmonic(fret: read())
                                        case 4 -> new PinchHarmonic()
                                        case 5 -> new SemiHarmonic()
                                        default -> throw new RuntimeException('Invalid harmonic effect type')
                                    } : null
                                    effect.trill = bool(noteEffectFlags & 0x2000) ? new TrillEffect(
                                        fret: read(),
                                        duration: new Duration(switch (read()) {
                                            case 1 -> Duration.SIXTEENTH
                                            case 2 -> Duration.THIRTY_SECOND
                                            case 3 -> Duration.SIXTY_FOURTH
                                            default -> throw new RuntimeException('Invalid trill effect duration')
                                        })
                                    ) : null
                                }
                                measure
                            }}

                            // gp5 additions
                            // beat = getBeat(voice, start)
                            short gp5beatFlags = readShort()
                            octave = switch (true) {
                                case bool(gp5beatFlags & 0x0010) -> Octave.OTTAVA
                                case bool(gp5beatFlags & 0x0020) -> Octave.OTTAVA_BASSA
                                case bool(gp5beatFlags & 0x0040) -> Octave.QUINDICESIMA
                                case bool(gp5beatFlags & 0x0100) -> Octave.QUINDICESIMA_BASSA
                                default -> Octave.NONE
                            }
                            display.breakBeam = bool(gp5beatFlags & 0x0001)
                            display.forceBeam = bool(gp5beatFlags & 0x0004)
                            display.forceBracket = bool(gp5beatFlags & 0x2000)
                            display.breakSecondaryTuplet = bool(gp5beatFlags & 0x1000)
                            display.beamDirection = bool(gp5beatFlags & 0x0002) ? VoiceDirection.DOWN : bool(gp5beatFlags & 0x0008) ? VoiceDirection.UP : VoiceDirection.NONE
                            display.tupletBracket = bool(gp5beatFlags & 0x0200) ? TupletBracket.START : bool(gp5beatFlags & 0x0400) ? TupletBracket.END : TupletBracket.NONE
                            display.breakSecondary = bool(gp5beatFlags & 0x0800) ? readBoolean() : false
                        }}
                    }}
                }
            }
        }

        close()

        song
    }

    private static int readNoteValue(Note note) {
        int fret = read()
        int value = (note.type == NoteType.TIE) ? {
            Measure parentMeasure = note.beat.voice.measure
            int voiceIndex = parentMeasure.voices.indexOf(note.beat.voice)
            parentMeasure.track.measures.reverse().indexed().findResult(-1) { i, measure ->
                Voice voice = measure.voices[voiceIndex]
                List<Beat> beats = (i == 0) ? voice.beats[0..voice.beats.indexOf(note.beat)] : voice.beats
                beats.reverse().findAll {
                    it.status != BeatStatus.EMPTY
                }.findResult { beat ->
                    beat.notes.find { it.string == note.string }?.value
                }
            }
        }() : fret
        value in (0..<100) ? value : 0
    }

    private static int unpackVelocity(int dyn) {
        Velocities.minVelocity + (Velocities.velocityIncrement * dyn) - Velocities.velocityIncrement
    }

    private String readVariableLengthStringField() {
        int contentLength = readInt()
        // todo get charset from user preferences
        readFixedLengthStringField(contentLength, contentLength, 'UTF-8')
    }

    private String readFixedLengthStringField() throws IOException {
        int fieldLength = readInt() - 1
        readFixedLengthStringField(fieldLength)
    }

    private String readFixedLengthStringField(int fieldLength) {
        int contentLength = read()
        readFixedLengthStringField(fieldLength, contentLength, 'UTF-8')
    }

    private String readFixedLengthStringField(int fieldLength, int contentLength, String charset) throws IOException{
        byte[] bytes = new byte[fieldLength > 0 ? fieldLength : contentLength]
        read(bytes)
        int length = contentLength in (0..bytes.length) ? contentLength : fieldLength
        try {
            new String(bytes, 0, length, charset).getBytes('UTF-8').with { new String(it, 'UTF-8') }
        } catch (Exception e) {
            e.printStackTrace()
            new String(bytes, 0, length)
        }
    }

    private int readInt() {
        byte[] bytes = new byte[4]
        read(bytes)
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt()
    }

    private short readShort() {
        byte[] bytes = new byte[2]
        read(bytes)
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getShort()
    }

    private int readDouble() {
        byte[] bytes = new byte[8]
        read(bytes)
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getDouble()
    }

    private boolean readBoolean() {
        read() != 0
    }

    private int readUnsignedByte() {
        read() & 0xff
    }

    private void skipBytes(int n) {
        read(new byte[n])
    }

    private static Tuple3 v(int v1, int v2, int v3) {
        Tuple.tuple(v1, v2, v3)
    }

    private static boolean bool(int x) { x != 0 }
}
