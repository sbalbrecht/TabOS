package tabos

class GP5InputStream extends DataInputStream {
    static final VERSIONS = [
        'FICHIER GUITAR PRO v5.00': v(5, 0, 0),
        'FICHIER GUITAR PRO v5.10': v(5, 1, 0),
    ]

    GP5InputStream(InputStream stream) { super(stream) }

    @Override
    int read() throws IOException { super.read() }

    def readSong() {
        Tuple version = VERSIONS[readVersion()]

        // todo if isClipboard copyClipboard?

        def scoreInformation = [
            title: readIntByteSizeString(),
            subtitle: readIntByteSizeString(),
            artist: readIntByteSizeString(),
            album: readIntByteSizeString(),
            wordsAuthor: readIntByteSizeString(),
            musicAuthor: readIntByteSizeString(),
            copyright: readIntByteSizeString(),
            tabAuthor: readIntByteSizeString(),
            instructions: readIntByteSizeString(),
            information: (0..<readInt()).collect {readIntByteSizeString() }.join('\n')
        ]

        def lyrics = [
            lyricTrackIndex: readInt(),
            lines: (0..<5).collect {[
                startFromBar: readInt(),
                line: readIntSizeString()
            ]}
        ]

        def rseMasterEffect = (version > v(5, 0, 0)) ? [
            masterVolume: readInt().tap {
                // unknown value -- not master reverb or eq preset
                readInt()
            },
            // 10 band eq: 32, 60, 125, 250, 500, 1k, 2k, 4k, 8k, 16k, PRE
            eq: (0..<11).collect{-read() / 10 }
        ] : [:]

        def pageSetup = new PageSetup().tap {
            dimensions.width = readInt()
            dimensions.height = readInt()
            margin.left = readInt()
            margin.right = readInt()
            margin.top = readInt()
            margin.bottom = readInt()
            scoreSizeProportion = readInt() / 100

            List<Integer> templateFlags = [read(), read()]
            title.visible = (templateFlags[0] & 0x01) != 0
            subtitle.visible = (templateFlags[0] & 0x02) != 0
            artist.visible = (templateFlags[0] & 0x04) != 0
            album.visible = (templateFlags[0] & 0x08) != 0
            words.visible = (templateFlags[0] & 0x10) != 0
            music.visible = (templateFlags[0] & 0x20) != 0
            words.visible = (templateFlags[0] & 0x40) != 0
            copyright1.visible = copyright2.visible = (templateFlags[0] & 0x80) != 0
            pageNumber.visible = (templateFlags[1] & 0x01) != 0

            title.template = readIntByteSizeString()
            subtitle.template = readIntByteSizeString()
            artist.template = readIntByteSizeString()
            album.template = readIntByteSizeString()
            words.template = readIntByteSizeString()
            music.template = readIntByteSizeString()
            wordsAndMusic.template = readIntByteSizeString()
            copyright1.template = readIntByteSizeString()
            copyright2.template = readIntByteSizeString()
            pageNumber.template = readIntByteSizeString()
        }

        def tempo = [
            marking: readIntByteSizeString(),
            tempoValue: readInt(),
            hideTempo: (version > v(5, 0, 0)) ? readBoolean() : false
        ]

        KeySignature keySignature = KeySignature.from(read(), 0)
//        skipBytes 3
        def keySigBytes = (new byte[3]).tap { read(it) }

        int octave = read()

        def midiChannels = (0..<64).collect { i ->
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
                bank = (short) (i == 9 ? 128 : 0)
                parameters = []
                // blank1: 1 byte
                // blank2: 1 byte
                skipBytes(2)
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

        if (rseMasterEffect) {
            rseMasterEffect.masterReverb = readInt()
        } else {
            skipBytes 4
        }

        int numMeasures = readInt()
        int numTracks = readInt()

        // fixme keySignatures[] and timeSignature work weird
        //   each header should have a keySignature instead of making this separate array
        //   timeSig is adjusted and cloned for each measure, evolving over the course of the score. keySigs works similarly
        KeySignature[] keySignatures = (new KeySignature[numMeasures]).tap { it ->
            if (numMeasures > 0) it[0] = keySignature
            it
        }

        def timeSignature = [:]
        def measureHeaders = (0..<numMeasures).collect { i ->
            if (i > 0) skipBytes 1
            def flags = readUnsignedByte()
            [
                number: i + 1,
                preciseStart: null,
                start: 960L, // Quarter Time
                tempo: [ quarterValue: 120 ],
                repeatOpen: (flags & 0x04) != 0,
                // fixme test changing timeSignature
                timeSignature: timeSignature.clone().tap { ts ->
                    if ((flags & 0x01) != 0) ts.numerator = read()
                    // todo much complexity in this denominator "duration"
                    if ((flags & 0x02) != 0) ts.denominator = read()
                    ts
                },
                // fixme check: if > -1 then x - 1,
                repeatClose: (flags & 0x08) != 0 ? (read() & 0xff) : 0,
                marker: (flags & 0x20) != 0 ? [
                    measure: i + 1,
                    title: readIntByteSizeString(),
                    color: [
                        r: readUnsignedByte(),
                        g: readUnsignedByte(),
                        b: readUnsignedByte()
                    ].tap { skipBytes 1 }
                ] : null,
                hasDoubleBar: (flags & 0x80) != 0
            ].tap { Map<String, ?> measureHeader ->
                if ((flags & 0x40) != 0) {
                    keySignatures[i] = KeySignature.from(read(), read())
                } else if (i > 0) {
                    keySignatures[i] = keySignatures[i - 1]
                }

                if ((flags & 0x01) != 0 || (flags & 0x02) != 0) {
                    measureHeader.timeSignature.beams = (new byte[4]).tap { read(it) }
                } else {
                    // todo set to previous header's beams
//                    measureHeader.timeSignature.beams = //...
                }

                if ((flags & 0x10) != 0) {
                    // https://github.com/Perlence/PyGuitarPro/blob/master/src/guitarpro/gp3.py#L237-L244
                    measureHeader.repeatAlternative = read()
                } else {
                    skipBytes 1
                }

                measureHeader.tripletFeel = read()
            }
        }

        def tracks = (1..numTracks).collect { trackNumber ->
            if (trackNumber == 1 || version == v(5, 0, 0)) skipBytes(1) // probably some data
            read().with { flags -> [
                number: trackNumber,
                lyrics: trackNumber == lyrics.lyricTrackIndex ? lyrics.lines : null,
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
                channelId: {
                    int gmChannel1 = readInt() - 1
                    int gmChannel2 = readInt() - 1
                    if (gmChannel1 in (0..<midiChannels.size())) {
                        def gmChannel1Parameter = [key: 'gm-channel-1', value: Integer.toString(gmChannel1)]
                        def gmChannel2Parameter = [key: 'gm-channel-2', value: Integer.toString(gmChannel1 == 9 ? gmChannel1 : gmChannel2)]
                        def mainChannel = midiChannels[gmChannel1]
                        mainChannel.id = midiChannels.find { ch ->
                            ch.parameters.find { param -> param == gmChannel1Parameter }
                        }?.id ?: 0
                        if (mainChannel.id <= 0) {
                            mainChannel.id = midiChannels.size() - 1
                            mainChannel.name = '' // todo createChannelNameFromProgram?
                            mainChannel.parameters.add(gmChannel1Parameter)
                            mainChannel.parameters.add(gmChannel2Parameter)
                            // song.channel = mainChannel fixme
                        }
                        mainChannel.id
                    }
                },
                fretCount: readInt(),
                offset: readInt(),
                color: [
                    r: readUnsignedByte(),
                    g: readUnsignedByte(),
                    b: readUnsignedByte()
                ].tap { skipBytes 1 },
                settings: readShort().with { settingsFlags -> new TrackSettings(
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
                )},
                rseAutoAccentuation: read(),
                channelBank: read(),
                rse: [ // fixme needs refactor
                    humanize: read().tap {
                        readInt() // ?
                        readInt() // ?
                        readInt() // ?
                        skipBytes 12 // ?
                    },
                    instrument: readInt(),
                    unknown: readInt(), // fixme ? mostly 1
                    soundBank: readInt(),
                    effectNumber: (version < v(5, 1, 0)) ? readShort().tap { skip 1 } : readInt(),
                    equalizer: (version < v(5, 1, 0)) ? null : (0..<4).collect{-read() / 10 },
                    instrumentEffect: (version < v(5, 1, 0)) ? null : readIntByteSizeString(),
                    instrumentEffectCategory: (version < v(5, 1, 0)) ? null : readIntByteSizeString(),
                ]
            ]}
        }

        skipBytes(version == v(5, 0, 0) ? 1 : 2)

        def start = 960
        // measures
        for (def header : measureHeaders) {
            header.start = start
            for (def track : tracks) {
                def measure = [
                    start: start,
                    voices: (0..<2).collect { voiceIdx -> [
                        beats: (0..readInt()).collect { beatIdx ->
                            def noteEffect = [:]
                            readUnsignedByte().with { beatFlags -> [
                                status: bool(beatFlags & 0x40) ? read() : 1,
                                duration: [
                                    value: 1 << (read() + 2),
                                    isDotted: bool(beatFlags & 0x01),
                                    tuplet: bool(beatFlags & 0x20) ? readInt().with { iTuplet -> [
                                        enters: iTuplet,
                                        times: highestOneBit(iTuplet)
                                    ]} : null
                                ],
                                chord: bool(beatFlags & 0x02) ? readBoolean().with { isGP4Chord ->
                                    if (isGP4Chord) {
                                        new Chord().tap {
                                            sharp = readBoolean()
                                            skipBytes 3 // ?
                                            root = new Pitch(read(), it.sharp ? Pitch.Intonation.SHARP : Pitch.Intonation.FLAT)
                                            type = ChordType.from(read())
                                            extension = ChordExtension.from(read())
                                            bass = new Pitch(readInt(), -1) // -1?
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
                                } : null,
                                text: bool(beatFlags & 0x04) ? readIntByteSizeString() : null
                            ].tap { beat ->
                                // beat effects
                                if (bool(beatFlags & 0x08)) {
                                    int beatEffectFlags = read()
                                    noteEffect.vibrato = bool(beatEffectFlags & 0x01)
                                    // artificial: 1, natural: 2
                                    noteEffect.harmonic = bool(beatEffectFlags & 0x04) ? 1 : bool(beatEffectFlags & 0x08) ? 2 : null
                                    beat.effects = new BeatEffect()
                                    beat.effects.vibrato = bool(beatEffectFlags & 0x02)
                                    beat.effects.fadeIn = bool(beatEffectFlags & 0x10)
                                    if (bool(beatEffectFlags & 0x20)) {
                                        beat.effects.slapEffect = SlapEffect.from(read())
                                        beat.effects.tremoloEffect = readInt().with { tremoloValue ->
                                            if (beat.effects.slapEffect) return null
                                            [
                                                value: tremoloValue,
                                                type: BendType.DIP,
                                                points: [
                                                    [0, 0],
                                                    [Math.round(12 / 2), Math.round(-tremoloValue / 25)],
                                                    [12, 0]
                                                ]
                                            ]
                                        }
                                    }
                                    if ((beatEffectFlags & 0x40) != 0) {
                                        beat.effects.stroke = [read(), read()].with { strokeUp, strokeDown ->
                                            if (strokeUp > 0) [
                                                direction: BeatStrokeDirection.DOWN, // swapped
                                                value: strokeUp // fixme map to duration
                                            ] else if (strokeDown > 0) [
                                                direction: BeatStrokeDirection.UP, // swapped
                                                value: strokeDown // fixme map to duration
                                            ] else []
                                        }
                                    }
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
                                    ).tap { mixTableChange ->
                                        mixTableChange.volume?.duration = read()
                                        mixTableChange.balance?.duration = read()
                                        mixTableChange.chorus?.duration = read()
                                        mixTableChange.reverb?.duration = read()
                                        mixTableChange.phaser?.duration = read()
                                        mixTableChange.tremolo?.duration = read()
                                        mixTableChange.tempo?.duration = read()
                                        mixTableChange.hideTempo = mixTableChange.tempo == null && version > v(5, 0, 0) && readBoolean()
                                        // gp4 additions
                                        def mixTableChangeFlags = read()
                                        mixTableChange.volume?.allTracks = (mixTableChangeFlags & 0x01) != 0
                                        mixTableChange.balance?.allTracks = (mixTableChangeFlags & 0x02) != 0
                                        mixTableChange.chorus?.allTracks = (mixTableChangeFlags & 0x04) != 0
                                        mixTableChange.reverb?.allTracks = (mixTableChangeFlags & 0x08) != 0
                                        mixTableChange.phaser?.allTracks = (mixTableChangeFlags & 0x10) != 0
                                        mixTableChange.tremolo?.allTracks = (mixTableChangeFlags & 0x20) != 0
                                        // gp5 additions
                                        mixTableChange.useRSE = (mixTableChangeFlags & 0x40) != 0
                                        mixTableChange.wah = new WahEffect(
                                            value: read(),
                                            display: (mixTableChangeFlags & 0x80) != 0
                                        )
                                        if (mixTableChange.instrument < 0) mixTableChange.rse = null
                                        // read rse effect
                                        if (version > v(5, 0, 0)) {
                                            def effect = readIntByteSizeString()
                                            def effectCategory = readIntByteSizeString()
                                            if (mixTableChange.rse) {
                                                mixTableChange.rse.effect = effect
                                                mixTableChange.rse.effectCategory = effectCategory
                                            }
                                        }
                                        mixTableChange
                                    }
                                }

                                // read notes
                                def stringFlags = read()
                                notes = []
                                for (def string : track.strings) {
                                    if (stringFlags & 1 << (7 - string.number as int)) {
                                        def note = [:]
                                        note.string = string.number
                                        note.beat = beat
                                        note.effect = [:]
                                        note.effect.heavyAccentuateNote = bool(stringFlags & 0x02)
                                        note.effect.ghostNote = bool(stringFlags & 0x04)
                                        note.effect.accentuatedNote = bool(stringFlags & 0x40)
                                        note.type = bool(stringFlags & 0x20) ? NoteType.from(read()) : NoteType.NORMAL
                                        note.velocity = bool(stringFlags & 0x10) ? unpackVelocity(read()) : null // todo default not null?
                                        if (bool(stringFlags & 0x20)) {
                                            def fret = read()
                                            // if type == tie
                                            def value = (note.type == 2) ? getTiedNoteValue(note) : fret
                                            note.value = (value >= 0 && value < 100) ? value : 0
                                        }
                                        note.effect.leftHandFinger = bool(stringFlags & 0x80) ? read() : null // todo enum?
                                        note.effect.rightHandFinger = bool(stringFlags & 0x80) ? read() : null // todo enum?
                                        note.durationPercent = bool(stringFlags & 0x01) ? readDouble() : null // todo default?
                                        note.swapAccidentals = bool(read() & 0x02)
//                                        note.duration = (stringFlags & 0x01) != 0 ? readSignedByte() : null
//                                        note.tuplet = (stringFlags & 0x01) != 0 ? readSignedByte() : null
                                        def noteEffectFlags1 = read()
                                        def noteEffectFlags2 = read()
                                        note.effects.hammer = bool(noteEffectFlags1 & 0x02)
                                        note.effects.letRing = bool(noteEffectFlags1 & 0x08)
                                        note.effects.staccato = bool(noteEffectFlags2 & 0x01)
                                        note.effects.palmMute = bool(noteEffectFlags2 & 0x02)
                                        note.effects.vibrato = bool(noteEffectFlags2 & 0x40)
                                        note.effects.bend = bool(noteEffectFlags1 & 0x01) ? readBend() : null
                                        note.effects.grace = bool(noteEffectFlags1 & 0x10) ? readGrace() : null
                                        note.effects.tremoloPicking = bool(noteEffectFlags2 & 0x04) ? readTremoloPicking() : null
                                        note.effects.slides = bool(noteEffectFlags2 & 0x08) ? readSlides() : []
                                        note.effects.harmonic = bool(noteEffectFlags2 & 0x10) ? readHarmonic() : null
                                        note.effects.trill = bool(noteEffectFlags2 & 0x20) ? readTrill() : null
                                        notes << note
                                    }
                                }
                            }}
                        }
                    ]}
                ]
            }
        }


        close()
    }

    private int getTiedNoteValue(Map note) {
        // fixme no reference to voice, measure...
        def voiceIndex = note.beat.voice.measure.voices.indexOf(note.beat.voice)
        measure.track.measures.reversed().indexed().find { i, measure ->
            def voice = measure.voices[voiceIndex]
            def beats = (i == 0) ? voice.beats(0..voice.beats.indexOf(note.beat)) : voice.beats
            for (def beat : beats.reversed()) {
                // != empty
                if (beat.status != 0) {
                    for (def prevNote : beat.notes) {
                        if (prevNote.string == note.string) {
                            return prevNote.accidental
                        }
                    }
                }
            }
            return null
        }
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

    private Map readBend() throws IOException {
        [
            type: read(),
            value: readInt(),
            points: (0..readInt()).collect {
                [
                    // maxPosition = 12, bendPosition = 60
                    position: Math.round(readInt() * 12 / 60),
                    // semitoneLength = 1, bendSemitone = 25
                    value: Math.round(readInt() * 1 / 25),
                    vibrato: readBoolean(),
                ]
            } ?: []
        ].with { bend -> bend.points ? bend : null }
    }

    private Map readGrace() throws IOException {
        [
            fret: read(),
            velocity: unpackVelocity(read()),
            transition: GraceEffectTransition.from(read()),
            duration: 1 << (7 - read()),
        ].tap { Map grace ->
            def graceFlags = read()
            isDead = (graceFlags & 0x01) != 0
            isOnBeat = (graceFlags & 0x02) != 0
        }
    }

    private Map readTremoloPicking()  {
        [
            // 1: eighth, 2: sixteenth, 3: thirty-second
            duration: read() // todo convert to duration class, map tremolo value code to duration
        ]
    }

    private List<Integer> readSlides() {
        def slideFlags = read()
        def slides = []
        if ((slideFlags & 0x01) != 0)
            slides << 1 // shiftSlideTo
        if ((slideFlags & 0x02) != 0)
            slides << 2 // legatoSlideTo
        if ((slideFlags & 0x04) != 0)
            slides << 3 // outDownwards
        if ((slideFlags & 0x08) != 0)
            slides << 4 // outUpwards
        if ((slideFlags & 0x10) != 0)
            slides << -1 // intoFromBelow
        if ((slideFlags & 0x20) != 0)
            slides << -2 // intoFromAbove
        slides
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

    private Map readTrill() {
        [
            fret: read(),
            duration: [
                value: read() // todo 1: sixteenth, 2: thirtysecond, 3: sixtyfourth; flesh out duration
            ]
        ]
    }

    private short readByteToShort() {
        return (short) Math.max(((read() * 8) - 1), 0)
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
