package tabos

class GP5InputStream extends DataInputStream {
    static final int BEND_POSITION = 60
    static final int BEND_SEMITONE = 25
    static final VERSIONS = [
        'FICHIER GUITAR PRO v5.00': v(5, 0, 0),
        'FICHIER GUITAR PRO v5.10': v(5, 1, 0),
    ]

    GP5InputStream(InputStream stream) { super(stream) }

    @Override
    int read() throws IOException { super.read() }

    def readSong() {
        final Song song = new Song()
        song.version = readVersion()
        final Tuple version = VERSIONS[song.version]

        // todo if isClipboard copyClipboard?
        song.title = readIntByteSizeString()
        song.subtitle = readIntByteSizeString()
        song.artist = readIntByteSizeString()
        song.album = readIntByteSizeString()
        song.wordsAuthor = readIntByteSizeString()
        song.musicAuthor = readIntByteSizeString()
        song.copyright = readIntByteSizeString()
        song.tabAuthor = readIntByteSizeString()
        song.instructions = readIntByteSizeString()
        song.notice = (0..<readInt()).collect {readIntByteSizeString() }
        song.lyrics = new Lyrics(
            lyricTrackIndex: readInt(),
            lines: (0..<5).collect {new LyricLine(
                startingMeasure: readInt(),
                line: readIntSizeString()
            )}
        )

        song.rseMasterEffect = version > v(5, 0, 0) ? new RSEMasterEffect(
            volume: readInt().tap {
                // unknown value -- not master reverb or eq preset
                readInt()
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
            List<Integer> templateFlags = [read(), read()]
            title = new HeaderElement(readIntByteSizeString(), bool(templateFlags[0] & 0x01))
            subtitle = new HeaderElement(readIntByteSizeString(), bool(templateFlags[0] & 0x02))
            artist = new HeaderElement(readIntByteSizeString(), bool(templateFlags[0] & 0x04))
            album = new HeaderElement(readIntByteSizeString(), bool(templateFlags[0] & 0x08))
            words = new HeaderElement(readIntByteSizeString(), bool(templateFlags[0] & 0x10))
            music = new HeaderElement(readIntByteSizeString(), bool(templateFlags[0] & 0x20))
            wordsAndMusic = new HeaderElement(readIntByteSizeString(), bool(templateFlags[0] & 0x40))
            copyright1 = new HeaderElement(readIntByteSizeString(), bool(templateFlags[0] & 0x80))
            copyright2 = new HeaderElement(readIntByteSizeString(), bool(templateFlags[0] & 0x80))
            pageNumber = new HeaderElement(readIntByteSizeString(), bool(templateFlags[1] & 0x01))
        }
        song.tempoName = readIntByteSizeString()
        song.tempo = readInt()
        song.hideTempo = (version > v(5, 0, 0)) ? readBoolean() : false
        song.key = KeySignature.from(read(), 0).tap {
            // skipBytes 3
            println "keySigBytes=${(new byte[3]).tap { read(it) }}"
        }

        int songOctave = read() // fixme where does this live

        List<MidiChannel> midiChannels = (0..<64).collect { i ->
            new MidiChannel().tap {
                channel = i
                effectChannel = i
                instrument = Math.max(readInt(), 0).with {
                    it == -1 && channel == DEFAULT_PERCUSSION_CHANNEL ? 0 : it
                }
                volume = read()
                balance = read()
                chorus = read()
                reverb = read()
                phaser = read()
                tremolo = read()
                bank = (short) (i == DEFAULT_PERCUSSION_CHANNEL ? 128 : 0)
            }
        }

        def directions = [
            signs: [
                coda: readShort(),
                doubleCoda: readShort(),
                segno: readShort(),
                segnoSegno: readShort(),
                fine: readShort()
            ],
            fromSigns: [
                daCapo: readShort(),
                daCapoAlCoda: readShort(),
                daCapoAlDoubleCoda: readShort(),
                daCapoAlFine: readShort(),
                daSegno: readShort(),
                daSegnoAlCoda: readShort(),
                daSegnoAlDoubleCoda: readShort(),
                daSegnoAlFine: readShort(),
                daSegnoSegno: readShort(),
                daSegnoSegnoAlCoda: readShort(),
                daSegnoSegnoAlDoubleCoda: readShort(),
                daSegnoSegnoAlFine: readShort(),
                daCoda: readShort(),
                daDoubleCoda: readShort()
            ]
        ]

        if (song.rseMasterEffect) {
            song.rseMasterEffect.reverb = readInt()
        } else {
            skipBytes 4
        }

        int numMeasures = readInt()
        int numTracks = readInt()

        // fixme keySignatures[] and timeSignature work weird
        //   each header should have a keySignature instead of making this separate array
        //   timeSig is adjusted and cloned for each measure, evolving over the course of the score. keySigs works similarly
        KeySignature[] keySignatures = (new KeySignature[numMeasures]).tap { it ->
            if (numMeasures > 0) it[0] = song.key
            it
        }

        def timeSignature = new TimeSignature()
        def measureHeaders = (0..<numMeasures).collect { i ->
            if (i > 0) skipBytes 1
            readUnsignedByte().with { flags -> new MeasureHeader(
                number: i + 1,
//                preciseStart: null,
                start: Duration.QUARTER_TIME,
//                tempo: [ quarterValue: 120 ],
                isRepeatOpen: bool(flags & 0x04),
                // fixme test changing timeSignature
                timeSignature: (timeSignature.clone() as TimeSignature).with {  ts ->
                    if (bool(flags & 0x01)) ts.numerator = read()
                    // fixme fromTime correct?
                    if (bool(flags & 0x02)) ts.denominator = Duration.fromTime(read())
                    ts
                },
                // fixme check: if > -1 then x - 1,
                repeatClose: bool(flags & 0x08) ? (read() & 0xff) : 0,
                marker: bool(flags & 0x20) ? new Marker(
                    title: readIntByteSizeString(),
                    color: new Color(
                        r: readUnsignedByte(),
                        g: readUnsignedByte(),
                        b: readUnsignedByte()
                    ).tap { skipBytes 1 }
                ) : null,
                hasDoubleBar: bool(flags & 0x80)
            ).tap { MeasureHeader measureHeader ->
                if (bool(flags & 0x40)) {
                    keySignatures[i] = KeySignature.from(read(), read())
                } else if (i > 0) {
                    keySignatures[i] = keySignatures[i - 1]
                }

                if (bool(flags & 0x01) || bool(flags & 0x02)) {
                    measureHeader.timeSignature.beams = (new byte[4]).tap { read(it) }
                } else {
                    // todo set to previous header's beams
//                    measureHeader.timeSignature.beams = //...
                }

                if (bool(flags & 0x10)) {
                    // https://github.com/Perlence/PyGuitarPro/blob/master/src/guitarpro/gp3.py#L237-L244
                    measureHeader.repeatAlternative = read()
                } else {
                    skipBytes 1
                }

                measureHeader.tripletFeel = TripletFeel.from(read())
            }}
        }

        song.tracks = (1..numTracks).collect { trackNumber ->
            if (trackNumber == 1 || version == v(5, 0, 0)) skipBytes(1) // probably some data
            read().with { flags -> new Track(
                song: song,
                number: trackNumber,
                isPercussionTrack: bool(flags & 0x01),
                is12StringedGuitarTrack: bool(flags & 0x02),
                isBanjoTrack: bool(flags & 0x04),
                isVisible: bool(flags & 0x08),
                isSolo: bool(flags & 0x10),
                isMute: bool(flags & 0x20),
                useRSE: bool(flags & 0x40),
                indicateTuning: bool(flags & 0x80),
                name: readByteSizeString(40),
                strings: readInt().with { stringCount ->
                    (0..<7).collect {
                        readInt()
                    }.indexed().collect { i, tuning -> new GuitarString(
                        number: i + 1,
                        tuning: tuning
                    )}[0..<stringCount]
                },
                port: readInt(),
            ).tap {
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
                settings = readShort().with { settingsFlags -> new TrackSettings(
                    tablature: bool(settingsFlags & 0x0001),
                    notation: bool(settingsFlags & 0x0002),
                    diagramsAreBelow: bool(settingsFlags & 0x0004),
                    showRhythm: bool(settingsFlags & 0x0008),
                    forceHorizontal: bool(settingsFlags & 0x0010),
                    forceChannels: bool(settingsFlags & 0x0020),
                    diagramList: bool(settingsFlags & 0x0040),
                    diagramsInScore: bool(settingsFlags & 0x0080),
                    unknown: bool(settingsFlags & 0x0100), // fixme 0x0100 ?
                    autoLetRing: bool(settingsFlags & 0x0200),
                    autoBrush: bool(settingsFlags & 0x0400),
                    extendRhythmic: bool(settingsFlags & 0x0800),
                )}
                rse = new TrackRSE()
                rse.autoAccentuation = Accentuation.from(read())
                def channelBank = read() // fixme ?
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
                    rse.equalizer = (version < v(5, 1, 0)) ? null : new RSEEqualizer((0..<4).collect{ (-read() / 10) as float })
                    rse.instrument.effect = (version < v(5, 1, 0)) ? null : readIntByteSizeString()
                    rse.instrument.effectCategory = (version < v(5, 1, 0)) ? null : readIntByteSizeString()
                }
                it
            }}
        }

        skipBytes(version == v(5, 0, 0) ? 1 : 2)

        def start = Duration.QUARTER_TIME
        // measures
        for (def header : measureHeaders) {
            header.start = start
            for (def track : song.tracks) {
                track.measures << new Measure().tap { measure ->
                    // start: start, fixme?
                    measure.header = header
                    voices = (0..<MAX_VOICES).collect { new Voice().tap { voice ->
                        voice.measure = measure
                        beats = (0..readInt()).collect {new Beat().tap { beat ->
                            final int beatFlags = readUnsignedByte()
                            beat.voice = voice
                            status = BeatStatus.from(bool(beatFlags & 0x40) ? read() : 1)
                            duration = new Duration(
                                value: 1 << (read() + 2),
                                isDotted: bool(beatFlags & 0x01),
                                tuplet: bool(beatFlags & 0x20) ? readInt().with { iTuplet -> new Tuplet(
                                    enters: iTuplet,
                                    times: highestOneBit(iTuplet)
                                )} : null
                            )
                            effect.chord = bool(beatFlags & 0x02) ? readBoolean().with { isGP4Chord ->
                                if (isGP4Chord) {
                                    new Chord().tap {
                                        newFormat = true
                                        sharp = readBoolean()
                                        skipBytes 3 // ?
                                        root = new Pitch(read(), it.sharp ? Pitch.Intonation.SHARP : Pitch.Intonation.FLAT)
                                        type = ChordType.from(read())
                                        extension = ChordExtension.from(read())
                                        bass = new Pitch(readInt(), -1) // fixme -1?
                                        tonality = ChordAlteration.from(readInt())
                                        add = readBoolean()
                                        name = readByteSizeString(22)
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
                                    }
                                } else {
                                    new Chord().tap {
                                        name = readIntByteSizeString()
                                        firstFret = readInt()
                                        strings = firstFret ? (0..<7).collect { i -> new GuitarString(i + 1, readInt()) }[0..<track.strings.size()] : [new GuitarString(-1, -1)] * track.strings.size()
                                    }
                                }
                            } : null
                            text = bool(beatFlags & 0x04) ? readIntByteSizeString() : null
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
                            // mix table change
                            if ((beatFlags & 0x10) != 0) {
                                Closure<MixTableItem> toMixTableItem = { int value -> value >= 0 ? new MixTableItem(value) : null }
                                beat.effect.mixTableChange = new MixTableChange(
                                    instrument: toMixTableItem(read()),
                                    // rse gp5
                                    rse: new RSEInstrument(
                                        instrument: readInt(),
                                        unknown: readInt(), // fixme ? mostly 1
                                        soundBank: readInt(),
                                        effectNumber: (version == v(5, 0, 0)) ? readShort().tap { skip 1 } : readInt(),
                                    ).tap { if (version == v(5, 0, 0)) skipBytes 1 },
                                    volume: toMixTableItem(read()),
                                    balance: toMixTableItem(read()),
                                    chorus: toMixTableItem(read()),
                                    reverb: toMixTableItem(read()),
                                    phaser: toMixTableItem(read()),
                                    tremolo: toMixTableItem(read()),
                                    tempoName: readIntByteSizeString(), // gp5
                                    tempo: toMixTableItem(readInt())
                                ).tap {
                                    volume?.duration = read()
                                    balance?.duration = read()
                                    chorus?.duration = read()
                                    reverb?.duration = read()
                                    phaser?.duration = read()
                                    tremolo?.duration = read()
                                    tempo?.duration = read()
                                    hideTempo = !tempo && version > v(5, 0, 0) && readBoolean()
                                    // gp4 additions
                                    def mixTableChangeFlags = read()
                                    volume?.allTracks = bool(mixTableChangeFlags & 0x01)
                                    balance?.allTracks = bool(mixTableChangeFlags & 0x02)
                                    chorus?.allTracks = bool(mixTableChangeFlags & 0x04)
                                    reverb?.allTracks = bool(mixTableChangeFlags & 0x08)
                                    phaser?.allTracks = bool(mixTableChangeFlags & 0x10)
                                    tremolo?.allTracks = bool(mixTableChangeFlags & 0x20)
                                    // gp5 additions
                                    useRSE = (mixTableChangeFlags & 0x40) != 0
                                    wah = new WahEffect(
                                        value: read(),
                                        display: (mixTableChangeFlags & 0x80) != 0
                                    )
                                    if (instrument < 0) rse = null
                                    // read rse effect
                                    if (version > v(5, 0, 0)) {
                                        def effect = readIntByteSizeString()
                                        def effectCategory = readIntByteSizeString()
                                        if (rse) {
                                            rse.effect = effect
                                            rse.effectCategory = effectCategory
                                        }
                                    }
                                    it
                                }
                            }

                            // read notes
                            def stringFlags = read()
                            notes = []
                            for (def string : track.strings) {
                                if (stringFlags & 1 << (7 - string.number())) {
                                    Note note = new Note()
                                    note.beat = beat
                                    note.string = string.number()
                                    note.effect = new NoteEffect()
                                    note.effect.heavyAccentuatedNote = bool(stringFlags & 0x02)
                                    note.effect.ghostNote = bool(stringFlags & 0x04)
                                    note.effect.accentuatedNote = bool(stringFlags & 0x40)
                                    note.type = bool(stringFlags & 0x20) ? NoteType.from(read()) : NoteType.NORMAL
                                    note.velocity = bool(stringFlags & 0x10) ? unpackVelocity(read()) : Velocities.defaultVelocity
                                    if (bool(stringFlags & 0x20)) {
                                        int fret = read()
                                        int value = (note.type == NoteType.TIE) ? getTiedNoteValue(note) : fret
                                        note.value = value in (0..<100) ? value : 0
                                    }
                                    note.effect.leftHandFinger = bool(stringFlags & 0x80) ? Fingering.from(read()) : null
                                    note.effect.rightHandFinger = bool(stringFlags & 0x80) ? Fingering.from(read()) : null
                                    note.durationPercent = bool(stringFlags & 0x01) ? readDouble() : 1.0
                                    note.swapAccidentals = bool(read() & 0x02)
                                    def noteEffectFlags = readShort()
                                    note.effect.hammer = bool(noteEffectFlags & 0x0002)
                                    note.effect.letRing = bool(noteEffectFlags & 0x0008)
                                    note.effect.staccato = bool(noteEffectFlags & 0x0100)
                                    note.effect.palmMute = bool(noteEffectFlags & 0x0200)
                                    note.effect.vibrato = bool(noteEffectFlags & 0x4000)
                                    note.effect.bend = bool(noteEffectFlags & 0x0001) ? readBend() : null
                                    note.effect.grace = bool(noteEffectFlags & 0x0010) ? readGrace() : null
                                    note.effect.tremoloPicking = bool(noteEffectFlags & 0x0400) ? readTremoloPicking() : null
                                    note.effect.slides = bool(noteEffectFlags & 0x0800) ? readSlides() : []
                                    note.effect.harmonic = bool(noteEffectFlags & 0x1000) ? readHarmonic() : null
                                    note.effect.trill = bool(noteEffectFlags & 0x2000) ? readTrill() : null
                                    notes << note
                                }
                            }

                            // gp5 additions
                            // beat = getBeat(voice, start)
                            short gp5beatFlags = readShort()
                            octave = {
                                if (bool(gp5beatFlags & 0x0010)) Octave.OTTAVA
                                else if (bool(gp5beatFlags & 0x0020)) Octave.OTTAVA_BASSA
                                else if (bool(gp5beatFlags & 0x0040)) Octave.OTTAVA_BASSA
                                else if (bool(gp5beatFlags & 0x0100)) Octave.OTTAVA_BASSA
                                else Octave.NONE
                            }()
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

    private static int getTiedNoteValue(Note note) {
        def parentMeasure = note.beat.voice.measure
        def voiceIndex = parentMeasure.voices.indexOf(note.beat.voice)
        for (def entry : parentMeasure.track.measures.reversed().indexed()) {
            def i = entry.key
            def measure = entry.value
            Voice voice = measure.voices[voiceIndex]
            List<Beat> beats = (i == 0) ? voice.beats[0..voice.beats.indexOf(note.beat)] : voice.beats
            for (def beat : beats.reversed()) {
                if (beat.status != BeatStatus.EMPTY) {
                    for (Note prevNote : beat.notes) {
                        if (prevNote.string == note.string) {
                            return prevNote.value
                        }
                    }
                }
            }
        }
        -1
    }

    private static int unpackVelocity(int dyn) {
        def minVelocity = 15
        def velocityIncrement = 16
        minVelocity + (velocityIncrement * dyn) - velocityIncrement
    }

    private String readVersion() throws IOException {
        int len = readUnsignedByte()
        byte[] bytes = new byte[30]
        read(bytes)
        new String(new String(bytes, 0, len in (0..30) ? len : 30, 'UTF-8').getBytes('UTF-8'), 'UTF-8')
    }

    private BendEffect readBend() throws IOException {
        new BendEffect(
            type: BendType.from(read()),
            value: readInt(),
            points: (0..readInt()).collect {
                new BendPoint(
                    position: Math.round(readInt() * BendEffect.MAX_POSITION / BEND_POSITION),
                    value: Math.round(readInt() * BendEffect.SEMITONE_LENGTH / BEND_SEMITONE),
                    vibrato: readBoolean(),
                )
            } ?: []
        ).with { bend -> bend.points ? bend : null }
    }

    private GraceEffect readGrace() throws IOException {
        new GraceEffect().tap {
            fret = read()
            velocity = unpackVelocity(read())
            transition = GraceEffectTransition.from(read())
            duration = 1 << (7 - read())
            def graceFlags = read()
            isDead = (graceFlags & 0x01) != 0
            isOnBeat = (graceFlags & 0x02) != 0
        }
    }

    private TremoloPickingEffect readTremoloPicking()  {
        new TremoloPickingEffect(
            duration: new Duration(
                value: read().with {
                    return switch (it) {
                        case 1 -> Duration.EIGHTH
                        case 2 -> Duration.SIXTEENTH
                        case 3 -> Duration.THIRTY_SECOND
                        default -> throw new RuntimeException("Invalid tremolo picking effect duration $it")
                    }
                }
            )
        )
    }

    private List<SlideType> readSlides() {
        read().with { slideFlags ->
            def slides = []
            if (bool(slideFlags & 0x01)) slides << SlideType.SHIFT_SLIDE_TO
            if (bool(slideFlags & 0x02)) slides << SlideType.LEGATO_SLIDE_TO
            if (bool(slideFlags & 0x04)) slides << SlideType.OUT_DOWNWARDS
            if (bool(slideFlags & 0x08)) slides << SlideType.OUT_UPWARDS
            if (bool(slideFlags & 0x10)) slides << SlideType.INTO_FROM_BELOW
            if (bool(slideFlags & 0x20)) slides << SlideType.INTO_FROM_BELOW
            slides
        }
    }

    private HarmonicEffect readHarmonic() {
        return switch (read()) {
            case 1 -> new NaturalHarmonic()
            case 2 -> new ArtificialHarmonic(
                pitch: new Pitch(read(), read()),
                octave: Octave.from(read())
            )
            case 3 -> new TappedHarmonic(fret: read())
            case 4 -> new PinchHarmonic()
            case 5 -> new SemiHarmonic()
            default -> null
        }
    }

    private TrillEffect readTrill() {
        new TrillEffect(
            fret: read(),
            duration: new Duration(
                value: read().with {
                    return switch (it) {
                        case 1 -> Duration.SIXTEENTH
                        case 2 -> Duration.SIXTEENTH
                        case 3 -> Duration.SIXTEENTH
                        default -> throw new RuntimeException("Invalid trill effect duration $it")
                    }
                }
            )
        )
    }

    private String readIntSizeString() {
        int length = readInt()
        readString(length, length, 'UTF-8')
    }

    private String readByteSizeString(int size) {
        readString(size, readUnsignedByte(), 'UTF-8')
    }

    private String readIntByteSizeString() throws IOException {
        readString(readInt() - 1, readUnsignedByte(), 'UTF-8')
    }

    private String readString(int size, int len, String charset) throws IOException{
        byte[] bytes = new byte[size > 0 ? size : len]
        read(bytes)
        newString(bytes, len in (0..bytes.length) ? len : size, charset).tap {
            println "readString size=$size length=$len text=$it"
        }
    }

    private static String newString(byte[] bytes, int length, String charset) {
        try {
            new String(new String(bytes, 0, length, charset).getBytes('UTF-8'), 'UTF-8')
        } catch (Throwable e) {
            e.printStackTrace()
            new String(bytes, 0, length)
        }
    }

    private static Tuple3 v(int v1, int v2, int v3) {
        Tuple.tuple(v1, v2, v3)
    }

    private static boolean bool(int x) { x != 0 }
}
