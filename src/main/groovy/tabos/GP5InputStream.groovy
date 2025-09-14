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

        // Project info
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

        def pageSetup = [:].tap {
            width = readInt()
            height = readInt()
            marginLeft = readInt()
            marginRight = readInt()
            marginTop = readInt()
            marginBottom = readInt()
            scoreSizeProportion = readInt() / 100

            List<Integer> templateFlags = [read(), read()]
            titleTemplateEnabled = (templateFlags[0] & 0x01) != 0
            subtitleTemplateEnabled = (templateFlags[0] & 0x02) != 0
            artistTemplateEnabled = (templateFlags[0] & 0x04) != 0
            albumTemplateEnabled = (templateFlags[0] & 0x08) != 0
            wordsTemplateEnabled = (templateFlags[0] & 0x10) != 0
            musicTemplateEnabled = (templateFlags[0] & 0x20) != 0
            wordsAndMusicEnabled = (templateFlags[0] & 0x40) != 0
            copyrightTemplateEnabled = (templateFlags[0] & 0x80) != 0
            pageNumberTemplateEnabled = (templateFlags[1] & 0x01) != 0

            titleTemplate = readIntByteSizeString()
            subtitleTemplate = readIntByteSizeString()
            artistTemplate = readIntByteSizeString()
            albumTemplate = readIntByteSizeString()
            wordsTemplate = readIntByteSizeString()
            musicTemplate = readIntByteSizeString()
            wordsAndMusicTemplate = readIntByteSizeString()
            copyright1Template = readIntByteSizeString()
            copyright2Template = readIntByteSizeString()
            pageNumberTemplate = readIntByteSizeString()
        }

        def tempo = [
            marking: readIntByteSizeString(),
            tempoValue: readInt(),
            hideTempo: (version > v(5, 0, 0)) ? readBoolean() : false
        ]

        // -7 to 7 -- normalize 0 to 14?
        int keySignature = read()
        // boolean "until the end" ?
        // mode major / minor? fixme
//        skipBytes 3
        def keySigBytes = (new byte[3]).tap { read(it) }

        int octave = read()

        def midiChannels = (0..<64).collect { i ->
            [
                id: 0,
                program: (short) Math.max(readInt(), 0), // rename to instrument?
                volume: readByteToShort(),
                balance: readByteToShort(),
                chorus: readByteToShort(),
                reverb: readByteToShort(),
                phaser: readByteToShort(),
                tremolo: readByteToShort(),
                bank: (short) (i == 9 ? 128 : 0),
                parameters: []
            ].tap {
                // blank1: byte
                // blank2: byte
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
        int[] keySignatures = (new int[numMeasures]).tap { it ->
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
                    // fixme normalize or no? -7 to 7 or 0 to 14?
                    def sig = [
                        key: read(),
                        isMajor: readBoolean()
                    ].tap { println "key signature isMajor=$it.isMajor keyCode=$it.keyCode" }
                    keySignatures[i] = sig.key
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
                isPercussionTrack: (flags & 0x01) != 0,
                is12StringedGuitarTrack: (flags & 0x02) != 0,
                isBanjoTrack: (flags & 0x04) != 0,
                isVisible: (flags & 0x08) != 0,
                isSolo: (flags & 0x10) != 0,
                isMute: (flags & 0x20) != 0,
                useRSE: (flags & 0x40) != 0,
                indicateTuning: (flags & 0x80) != 0,
                name: readByteSizeString(40),
                strings: readInt().with { stringCount ->
                    (0..<7).collect {
                        readInt()
                    }.indexed().collect { i, tuning -> [
                        number: i + 1,
                        tuning: tuning
                    ]}[0..<stringCount]
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
                settings: readShort().with { settingsFlags -> [
                    tablature: (settingsFlags & 0x0001) != 0,
                    notation: (settingsFlags & 0x0002) != 0,
                    diagramsAreBelow: (settingsFlags & 0x0004) != 0,
                    showRhythm: (settingsFlags & 0x0008) != 0,
                    forceHorizontal: (settingsFlags & 0x0010) != 0,
                    forceChannels: (settingsFlags & 0x0020) != 0,
                    diagramList: (settingsFlags & 0x0040) != 0,
                    digramsInScore: (settingsFlags & 0x0080) != 0,
                    unknown: (settingsFlags & 0x0100) != 0, // fixme 0x0100 ?
                    autoLetRing: (settingsFlags & 0x0200) != 0,
                    autoBrush: (settingsFlags & 0x0400) != 0,
                    extendRhythmic: (settingsFlags & 0x0800) != 0,
                ]},
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
                                status: (beatFlags & 0x40) != 0 ? read() : 1,
                                duration: [
                                    value: 1 << (read() + 2),
                                    isDotted: (beatFlags & 0x01) != 0,
                                    tuplet: (beatFlags & 0x20) != 0 ? readInt().with { iTuplet -> [
                                        enters: iTuplet,
                                        times: highestOneBit(iTuplet)
                                    ]} : null
                                ],
                                chord: (beatFlags & 0x02) != 0 ? readBoolean().with { isGP4Chord ->
                                    if (isGP4Chord) {
                                        [:].tap {
                                            sharp = readBoolean()
                                            skipBytes 3 // ?
                                            root = [
                                                value: read(),
                                                intonation: it.sharp ? 'sharp' : 'flat'
                                            ]
                                            type = read()
                                            extension = read()
                                            bass = readInt()
                                            tonality = readInt()
                                            add = readBoolean()
                                            name = readByteSizeString(22)
                                            fifth = read()
                                            ninth = read()
                                            eleventh = read()
                                            firstFret = readInt()
                                            strings = (0..<7).collect { readInt() }[0..<track.strings.size()]
                                            barres = read().with { barresCount -> [
                                                (0..<5).collect { read() },
                                                (0..<5).collect { read() },
                                                (0..<5).collect { read() },
                                            ].transpose()[0..<barresCount].collect { (barreFret, barreStart, barreEnd) -> [
                                                fret: barreFret,
                                                start: barreStart,
                                                end: barreEnd
                                            ]}}
                                            omissions = (0..<7).collect { readBoolean() }
                                            skipBytes 1
                                            fingerings = (0..<7).collect { read() } // todo to enum
                                            show = readBoolean()
                                        }
                                    } else {
                                        [
                                            name: readIntByteSizeString(),
                                            firstFret: readInt()
                                        ].tap { Map chord ->
                                            chord.strings = chord.firstFret ? (0..6).collect { readInt() }[0..<track.strings.size()] : [-1] * track.strings.size()
                                        }
                                    }
                                } : null,
                                text: (beatFlags & 0x04) != 0 ? readIntByteSizeString() : null
                            ].tap { beat ->
                                // beat effects
                                if ((beatFlags & 0x08) != 0) {
                                    int beatEffectFlags = read()
                                    noteEffect.vibrato = (beatEffectFlags & 0x01) != 0
                                    // artificial: 1, natural: 2
                                    noteEffect.harmonic = (beatEffectFlags & 0x04) != 0 ? 1 : (beatEffectFlags & 0x08) ? 2 : null
                                    beat.effects = [:]
                                    beat.effects.vibrato = (beatEffectFlags & 0x02) != 0
                                    beat.effects.fadeIn = (beatEffectFlags & 0x10) != 0
                                    if ((beatEffectFlags & 0x20) != 0) {
                                        beat.effects.slapEffect = read()
                                        beat.effects.tremoloEffect = readInt().with { tremoloValue ->
                                            if (beat.effects.slapEffect) return null
                                            [
                                                value: tremoloValue,
                                                type: 6, // BendType.dip
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
                                                direction: 2, // fixme to enum (swapped)
                                                value: strokeUp // fixme map to duration
                                            ] else if (strokeDown > 0) [
                                                direction: 1, // fixme to enum (swapped)
                                                value: strokeDown // fixme map to duration
                                            ] else []
                                        }
                                    }
                                }
                                // mix table change
                                if ((beatFlags & 0x10) != 0) {
                                    def toMixTableItem = { value -> value >= 0 ? [ value: value, duration: 0, allTracks: false ] : null }
                                    beat.effect.mixTableChange = [
                                        instrument: toMixTableItem(read()),
                                        // rse gp5
                                        rse: [
                                            instrument: readInt(),
                                            unknown: readInt(), // fixme ? mostly 1
                                            soundBank: readInt(),
                                            effectNumber: (version == v(5, 0, 0)) ? readShort().tap { skip 1 } : readInt(),
                                        ].tap { if (version == v(5, 0, 0)) skipBytes 1 },
                                        volume: toMixTableItem(read()),
                                        balance: toMixTableItem(read()),
                                        chorus: toMixTableItem(read()),
                                        reverb: toMixTableItem(read()),
                                        phaser: toMixTableItem(read()),
                                        tremolo: toMixTableItem(read()),
                                        tempoName: readIntByteSizeString(), // gp5
                                        tempo: toMixTableItem(readInt())
                                    ].tap { mixTableChange ->
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
                                        mixTableChange.wah = [
                                            value: read(),
                                            display: (mixTableChangeFlags & 0x80) != 0
                                        ]
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
                                        note.effect.heavyAccentuateNote = (stringFlags & 0x02) != 0
                                        note.effect.ghostNote = (stringFlags & 0x04) != 0
                                        note.effect.accentuatedNote = (stringFlags & 0x40) != 0
                                        note.type = (stringFlags & 0x20) != 0 ? read() : 1 // todo enum
                                        note.velocity = (stringFlags & 0x10) != 0 ? unpackVelocity(read()) : null // todo default not null?
                                        if ((stringFlags & 0x20) != 0) {
                                            def fret = read()
                                            // if type == tie
                                            def value = (note.type == 2) ? getTiedNoteValue(note) : fret
                                            note.value = (value >= 0 && value < 100) ? value : 0
                                        }
                                        note.effect.leftHandFinger = (stringFlags & 0x80) != 0 ? read() : null // todo enum?
                                        note.effect.rightHandFinger = (stringFlags & 0x80) != 0 ? read() : null // todo enum?
                                        note.durationPercent = (stringFlags & 0x01) != 0 ? readDouble() : null // todo default?
                                        note.swapAccidentals = (read() & 0x02) != 0
//                                        note.duration = (stringFlags & 0x01) != 0 ? readSignedByte() : null
//                                        note.tuplet = (stringFlags & 0x01) != 0 ? readSignedByte() : null
                                        def noteEffectFlags1 = read()
                                        def noteEffectFlags2 = read()
                                        note.effects.hammer = (noteEffectFlags1 & 0x02) != 0
                                        note.effects.letRing = (noteEffectFlags1 & 0x08) != 0
                                        note.effects.staccato = (noteEffectFlags2 & 0x01) != 0
                                        note.effects.palmMute = (noteEffectFlags2 & 0x02) != 0
                                        note.effects.vibrato = (noteEffectFlags2 & 0x40) != 0
                                        note.effects.bend = (noteEffectFlags1 & 0x01) != 0 ? readBend() : null
                                        note.effects.grace = (noteEffectFlags1 & 0x10) != 0 ? readGrace() : null
                                        note.effects.tremoloPicking = (noteEffectFlags2 & 0x04) != 0 ? readTremoloPicking() : null
                                        note.effects.slides = (noteEffectFlags2 & 0x08) != 0 ? readSlides() : []
                                        note.effects.harmonic = (noteEffectFlags2 & 0x10) != 0 ? readHarmonic() : null
                                        note.effects.trill = (noteEffectFlags2 & 0x20) != 0 ? readTrill() : null
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
                            return prevNote.value
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
            transition: read(), // todo enum GraceEffectTransition
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

    private Map readHarmonic() {
        def harmonicType = read()
        if (harmonicType == 1) {
            // natural
            return [ type: harmonicType ]
        } else if (harmonicType == 2) {
            // C = 0, D = 2, E = 4, F = 5, ...
            // b = -1, # = 1
            // loco = 0, 8va = 1, 15ma = 2
            // artificial
            return [
                type: harmonicType,
                pitch: [
                    semitone: read(),
                    accidental: read()
                ], // todo class?
                octave: read() // todo enum
            ]
        } else if (harmonicType == 3) {
            // tapped
            return [
                type: harmonicType,
                fret: read()
            ]
        } else if (harmonicType == 4) {
            // pinch
            return [ type: harmonicType ]
        } else if (harmonicType == 5) {
            // semi
            return [ type: harmonicType ]
        }
        return null
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
}
