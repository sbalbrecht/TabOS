package tabos

import groovy.transform.TupleConstructor

/*
Song
  Tracks
    Measures
      Voices
        Beats
          Notes
 */

class Song {
    String version // todo version tuple?
    // todo clipboard?
    String title
    String subtitle
    String artist
    String album
    String wordsAuthor
    String musicAuthor
    String copyright
    String tabAuthor
    String instructions
    List<String> notice
    Lyrics lyrics
    PageSetup pageSetup
    String tempoName = 'Moderate'
    int tempo = 120
    boolean hideTempo = false
    KeySignature key = KeySignature.C_MAJOR
    List<MeasureHeader> measureHeaders = []
    List<Track> tracks = []
    RSEMasterEffect rseMasterEffect

    private RepeatGroup currentRepeatGroup = new RepeatGroup()

    MeasureHeader addMeasureHeader(MeasureHeader header) {
        header.song = this
        measureHeaders << header
        if (header.isRepeatOpen || currentRepeatGroup.isClosed() && header.repeatAlternative <= 0) {
            currentRepeatGroup = new RepeatGroup()
        }
        currentRepeatGroup.addMeasureheader(header)
    }

    void newMeasure() {
        def header = new MeasureHeader()
        measureHeaders << header
        tracks.each {track -> track.measures << new Measure(track, header) }
    }
}

@TupleConstructor
class HeaderElement {
    String template = ''
    boolean visible = true
}

class PageSetup {
    def dimensions = [width: 210, height: 297]
    def margin = [left: 10, top: 15, right: 10, bottom: 10]
    float scoreSizeProportion = 1.0
    HeaderElement title = new HeaderElement('%title%')
    HeaderElement subtitle = new HeaderElement('%subtitle%')
    HeaderElement artist = new HeaderElement('%artist%')
    HeaderElement album = new HeaderElement('%album%')
    HeaderElement words = new HeaderElement('Words by %words%')
    HeaderElement music = new HeaderElement('Music by %music%')
    HeaderElement wordsAndMusic = new HeaderElement('Words & Music by %WORDSMUSIC%')
    HeaderElement copyright1 = new HeaderElement('Copyright %copyright%')
    HeaderElement copyright2 = new HeaderElement('All Rights Reserved - International Copyright Secured')
    HeaderElement pageNumber = new HeaderElement('Page %N%/%P%')
}

@TupleConstructor
class LyricLine {
    int startingMeasure = 1
    String line = ''
}

@TupleConstructor
class Lyrics {
    static final int MAX_LINES = 5
    int lyricTrackIndex = 0
    List<LyricLine> lines = []
}

@TupleConstructor
class MidiChannel {
    static final int DEFAULT_PERCUSSION_CHANNEL = 9
    int channel = 0
    int effectChannel = 1
    int instrument = 25
    int volume = 104
    int balance = 64
    int chorus = 0
    int reverb = 0
    int phaser = 0
    int tremolo = 0
    int bank = 0
}

@TupleConstructor
class RSEMasterEffect {
    float volume
    float reverb
    RSEEqualizer equalizer
}

class RepeatGroup {
    List<MeasureHeader> headers = []
    List<MeasureHeader> openings = []
    List<MeasureHeader> closings = []
    boolean isClosed = false
    void addMeasureHeader(MeasureHeader header) {
        if (!openings) openings << header
        headers << header
        header.repeatGroup = this
        if (header.repeatClose > 0) {
            closings << header
            isClosed = true
        } else if (isClosed) {
            openings << header
            isClosed = false
        }
    }
}

@TupleConstructor
class Tuplet {
    static final def supportedTuplets = [
        [1, 1],
        [3, 2],
        [5, 4],
        [6, 4],
        [7, 4],
        [9, 8],
        [10, 8],
        [11, 8],
        [12, 8],
        [13, 8],
    ]

    int enters = 1
    int times = 1

    def convertTime(int time) { (time * times / enters) as int }

    def isSupported() { enters <= 3 && enters >= 1 && times <= 3 && times >= 1 }

    static Tuplet fromFraction(Fraction frac) {
        new Tuplet(frac.denominator, frac.numerator)
    }
}

@TupleConstructor
class Duration {
    static final int QUARTER_TIME = 960
    static final int WHOLE = 1
    static final int HALF = 2
    static final int QUARTER = 4
    static final int EIGHTH = 8
    static final int SIXTEENTH = 16
    static final int THIRTY_SECOND = 32
    static final int SIXTY_FOURTH = 64
    static final int HUNDRED_TWENTY_EIGHTH = 128
    static final int MIN_TIME = (QUARTER_TIME * 4 / SIXTY_FOURTH * 2 / 3) as int

    int value = QUARTER
    boolean isDotted = false
    Tuplet tuplet = new Tuplet()

    def getTime() {
        tuplet.convertTime(
            ((QUARTER_TIME * 4 / value) as int).with {
                isDotted ? (it + it / 2) as int : it
            }
        )
    }

    int getIndex() { 32 - Integer.numberOfLeadingZeros(value) - 1 }

    static Duration fromTime(int time) {
        Fraction timeFrac = new Fraction(time, QUARTER_TIME * 4)
        def exp = (Math.log(timeFrac as double) / Math.log(2)) as int
        int value = (int) Math.pow(2, -exp)
        def tuplet = Tuplet.fromFraction(timeFrac * value)

        if (tuplet.isSupported()) return new Duration(value, false, tuplet)

        timeFrac = new Fraction(time, QUARTER_TIME * 4) * new Fraction(2, 3)
        exp = (Math.log(timeFrac as double) / Math.log(2)) as int
        value = Math.pow(2, -exp) as int
        tuplet = Tuplet.fromFraction(timeFrac * value)

        if (tuplet.isSupported()) new Duration(value, true, tuplet)

        throw new IllegalArgumentException("Cannot represent time $time as a duration")
    }
}

record Color(int r, int g, int b) {
    static final Color RED = new Color(255, 255, 255)
}

class Track {
    Song song = null
    int number = 1
    int fretCount = 24
    int offset = 0
    boolean isPercussionTrack = false
    boolean is12StringedGuitarTrack = false
    boolean isBanjoTrack = false
    boolean isVisible = true
    boolean isSolo = false
    boolean isMute = false
    boolean indicateTuning = false
    String name = 'Track 1'
    List<Measure> measures = []
    // (1, 64), (2, 59), (3, 55), (4, 50), (5, 45), (6, 40)
    List<GuitarString> strings = []
    int port = 1
    MidiChannel channel = null
    Color color = Color.RED
    TrackSettings settings
    boolean useRSE = false
    TrackRSE rse
}

@TupleConstructor
class TrackSettings {
    boolean tablature = true
    boolean notation = true
    boolean diagramsAreBelow = false
    boolean showRhythm = false
    boolean forceHorizontal = false
    boolean forceChannels = false
    boolean diagramList = true
    boolean diagramsInScore = false
    boolean unknown = false
    boolean autoLetRing = false
    boolean autoBrush = false
    boolean extendRhythmic = false
}

class RSEEqualizer {
    // 10 band eq: 32, 60, 125, 250, 500, 1k, 2k, 4k, 8k, 16k, PRE
    List<Float> knobs = [0.0] * 10
    Float gain = 0.0f
    RSEEqualizer(List<Float> values) {
        knobs = values[0..<-1]
        gain = values[-1]
    }
}

@TupleConstructor
class RSEInstrument {
    int instrument = -1
    int unknown = -1
    int soundBank = -1
    int effectNumber = -1
    String effect = ''
    String effectCategory = ''
}

class TrackRSE {
    RSEInstrument instrument
    RSEEqualizer equalizer
    int humanize = 0
    Accentuation autoAccentuation = Accentuation.NONE
}

enum TripletFeel {
    NONE(0),
    EIGHTH(1),
    SIXTEENTH(2),
    final int value
    TripletFeel(int value) { this.value = value }
    static from(int value) { values().find{ it.value == value } }
}

class TimeSignature {
    int numerator = 4
    Duration denominator
    List<Integer> beams = [2, 2, 2, 2]
}

enum Accentuation {
    NONE(0),
    VERY_SOFT(1),
    SOFT(2),
    MEDIUM(3),
    STRONG(4),
    VERY_STRONG(5),
    final int value
    Accentuation(int value) { this.value = value }
    static from(int value) { values().find{ it.value == value } }
}

record GuitarString(int number, int value) {
    @Override String toString() { 'C C# D D# E F F# G G# A A# B'.split()[value % 12] + value.intdiv(12) }
}

enum MeasureClef {
    TREBLE(0),
    BASS(1),
    TENOR(2),
    ALTO(3),
    final int value
    MeasureClef(int value) { this.value = value }
    static from(int value) { values().find{ it.value == value } }
}

enum KeySignature {
    F_MAJOR_FLAT(-8, 0),
    C_MAJOR_FLAT(-7, 0),
    G_MAJOR_FLAT(-6, 0),
    D_MAJOR_FLAT(-5, 0),
    A_MAJOR_FLAT(-4, 0),
    E_MAJOR_FLAT(-3, 0),
    B_MAJOR_FLAT(-2, 0),
    F_MAJOR(-1, 0),
    C_MAJOR(0, 0),
    G_MAJOR(1, 0),
    D_MAJOR(2, 0),
    A_MAJOR(3, 0),
    E_MAJOR(4, 0),
    B_MAJOR(5, 0),
    F_MAJOR_SHARP(6, 0),
    C_MAJOR_SHARP(7, 0),
    G_MAJOR_SHARP(8, 0),

    D_MINOR_FLAT(-8, 1),
    A_MINOR_FLAT(-7, 1),
    E_MINOR_FLAT(-6, 1),
    B_MINOR_FLAT(-5, 1),
    F_MINOR(-4, 1),
    C_MINOR(-3, 1),
    G_MINOR(-2, 1),
    D_MINOR(-1, 1),
    A_MINOR(0, 1),
    E_MINOR(1, 1),
    B_MINOR(2, 1),
    F_MINOR_SHARP(3, 1),
    C_MINOR_SHARP(4, 1),
    G_MINOR_SHARP(5, 1),
    D_MINOR_SHARP(6, 1),
    A_MINOR_SHARP(7, 1),
    E_MINOR_SHARP(8, 1),
    final int value
    final int isMinor
    private KeySignature(int value, int isMinor) {
        this.value = value
        this.isMinor = isMinor
    }
    static from(int value, int isMinor) { values().find{ it.value == value && it.isMinor == isMinor } }
}

enum BeatStrokeDirection {
    NONE(0),
    UP(1),
    DOWN(2),
    final int value
    BeatStrokeDirection(int value) { this.value = value }
    static from(int value) { values().find{ it.value == value } }
}

enum SlapEffect {
    NONE(0),
    TAPPING(1),
    SLAPPING(2),
    POPPING(3),
    final int value
    SlapEffect(int value) { this.value = value }
    static from(int value) { values().find{ it.value == value } }
}

enum SlideType {
    INTO_FROM_ABOVE(-2),
    INTO_FROM_BELOW(-1),
    NONE(0),
    SHIFT_SLIDE_TO(1),
    LEGATO_SLIDE_TO(2),
    OUT_DOWNWARDS(3),
    OUT_UPWARDS(4),
    final int value
    SlideType(int value) { this.value = value }
    static from(int value) { values().find{ it.value == value } }
}

enum Fingering {
    OPEN(-1),
    THUMB(0),
    INDEX(1),
    MIDDLE(2),
    ANNULAR(3),
    LITTLE(4),
    final int value
    Fingering(int value) { this.value = value }
    static from(int value) { values().find{ it.value == value } }
}

enum NoteType {
    REST(0),
    NORMAL(1),
    TIE(2),
    DEAD(3),
    final int value
    NoteType(int value) { this.value = value }
    static from(int value) { values().find{ it.value == value } }
}

class Voice {
    Measure measure
    List<Beat> beats = []
    VoiceDirection direction = VoiceDirection.NONE
    def isEmpty() { beats.isEmpty() }
}



class Beat {
    Voice voice
    List<Note> notes = []
    Duration duration = new Duration()
    String text
    Integer start
    BeatEffect effect = new BeatEffect()
    Octave octave = Octave.NONE
    BeatDisplay display = new BeatDisplay()
    BeatStatus status = BeatStatus.EMPTY

    def getStartInMeasure() {
        start - voice.measure.start
    }

    def hasVibrato() {
        notes.any { note -> note.effect.vibrato }
    }

    def hasHarmonic() {
        notes.find { note -> note.effect.isHarmonic }?.effect?.harmonic
    }
}

enum TupletBracket {
    NONE(0),
    START(1),
    END(2)
    final int value
    TupletBracket(int value) { this.value = value }
    static from(int value) { values().find{ it.value == value } }
}

class BeatDisplay {
    boolean breakBeam = false
    boolean forceBeam = false
    boolean beamDirection = false
    boolean tupletBracket = false
    boolean forceBracket = false
    boolean breakSecondary = false
    boolean breakSecondaryTuplet = false
}

class Marker {
    String title = 'Section'
    Color color = Color.RED
}

@TupleConstructor
class MeasureHeader {
    Song song
    RepeatGroup repeatGroup // fixme is this right
    int number
    int start
    boolean hasDoubleBar = false
    KeySignature keySignature = KeySignature.C_MAJOR
    TimeSignature timeSignature
    Marker marker
    boolean isRepeatOpen = false
    int repeatAlternative = 0
    int repeatClose = -1
    TripletFeel tripletFeel = TripletFeel.NONE
    String direction // todo enum?
    String fromDirection // todo enum?
    int length() { timeSignature.numerator + timeSignature.denominator.time }
}

@TupleConstructor
class GraceEffect {
    int duration = 32
    int fret = 0
    boolean isDead = false
    boolean isOnBeat = false
    GraceEffectTransition transition = GraceEffectTransition.NONE
    int velocity = Velocities.defaultVelocity
    int durationTime() { Duration.QUARTER_TIME * 4 / duration }
}

class TremoloPickingEffect {
    Duration duration
}

class TrillEffect {
    int fret = 0
    Duration duration
}


class NoteEffect {
    boolean accentuatedNote = false
    BendEffect bend
    boolean ghostNote = false
    GraceEffect grace
    boolean hammer = false
    HarmonicEffect harmonic
    boolean heavyAccentuatedNote = false
    Fingering leftHandFinger = Fingering.OPEN
    boolean letRing = false
    boolean palmMute = false
    Fingering rightHandFinger = Fingering.OPEN
    List<SlideType> slides = []
    boolean staccato = false
    TremoloPickingEffect tremoloPicking
    TrillEffect trill
    boolean vibrato = false

    boolean getIsBend() { bend != null && !bend.points.isEmpty() }
    boolean getIsHarmonic() { harmonic != null }
    boolean getIsGrace() { grace != null }
    boolean getIsTrill() { trill != null }
    boolean getIsTremoloPicking() { tremoloPicking != null }
    boolean getIsFingering() { leftHandFinger.value > -1 || rightHandFinger.value > -1 }
    boolean isDefault() {
        new NoteEffect().with { it ->
            this.leftHandFinger == it.leftHandFinger
                && this.rightHandFinger == it.rightHandFinger
                && this.bend == it.bend
                && this.harmonic == it.harmonic
                && this.grace == it.grace
                && this.trill == it.trill
                && this.tremoloPicking == it.tremoloPicking
                && this.vibrato == it.vibrato
                && this.slides == it.slides
                && this.hammer == it.hammer
                && this.palmMute == it.palmMute
                && this.staccato == it.staccato
                && this.letRing == it.letRing
        }
    }
}

class Note {
    Beat beat
    int value = 0
    int velocity = Velocities.defaultVelocity
    int string = 0
    NoteEffect effect = new NoteEffect()
    double durationPercent = 1.0f
    boolean swapAccidentals = false
    NoteType type = NoteType.REST
    int getRealValue() { value + beat.voice.measure.track.strings[string - 1].value() }
}

@TupleConstructor
class Measure {
    static final int MAX_VOICES = 2
    Track track
    MeasureHeader header
    MeasureClef clef = MeasureClef.TREBLE
    List<Voice> voices
    LineBreak lineBreak = LineBreak.NONE
    boolean isEmpty() { return voices.every { voice -> voice.isEmpty }}
}

enum LineBreak {
    NONE(0),
    BREAK(1),
    PROTECT(2),
    final int value
    LineBreak(int value) { this.value = value }
    static from(int value) { values().find{ it.value == value } }
}

enum VoiceDirection {
    NONE(0),
    UP(1),
    DOWN(2),
    final int value
    VoiceDirection(int value) { this.value = value }
    static from(int value) { values().find{ it.value == value } }
}

enum Octave {
    // loco = 0, 8va = 1, 15ma = 2   ??
    NONE(0),
    OTTAVA(1),
    QUINDICESIMA(2),
    OTTAVA_BASSA(3),
    QUINDICESIMA_BASSA(4),
    final int value
    Octave(int value) { this.value = value }
    static Octave from(int value) { values().find{ it.value == value } }
}

enum ChordType {
    MAJOR(0),
    SEVENTH(1),
    MAJOR_SEVENTH(2),
    SIXTH(3),
    MINOR(4),
    MINOR_SEVENTH(5),
    MINOR_MAJOR(6),
    MINOR_SIXTH(7),
    SUSPENDED_SECOND(8),
    SUSPENDED_FOURTH(9),
    SEVENTH_SUSPENDED_SECOND(10),
    SEVENTH_SUSPENDED_FOURTH(11),
    DIMINISHED(12),
    AUGMENTED(13),
    POWER(14)
    int value
    ChordType(int value) { this.value = value }
    static ChordType from(int value) { values().find { it.value == value }}
}

enum ChordAlteration {
    PERFECT(0),
    DIMINISHED(1),
    AUGMENTED(2)
    int value
    ChordAlteration(int value) { this.value = value }
    static ChordAlteration from(int value) { values().find { it.value == value }}
}

enum ChordExtension {
    NONE(0),
    NINTH(1),
    ELEVENTH(2),
    THIRTEENTH(3),
    int value
    ChordExtension(int value) { this.value = value }
    static ChordExtension from(int value) { values().find { it.value == value }}
}

class Barre {
    int fret
    int start
    int end
}

class Chord {
    int length
    Boolean sharp
    Pitch root
    ChordType type
    ChordExtension extension
    Pitch bass
    ChordAlteration tonality
    Boolean add
    String name = ''
    ChordAlteration fifth
    ChordAlteration ninth
    ChordAlteration eleventh
    Integer firstFret
    List<GuitarString> strings = []
    List<Barre> barres = []
    List<Boolean> omissions = []
    List<Fingering> fingerings = []
    Boolean show
    Boolean newFormat
}

class BeatEffect {
    BeatStroke stroke
    boolean hasRasgueado = false
    BeatStrokeDirection pickStroke = BeatStrokeDirection.NONE
    Chord chord
    boolean fadeIn = false
    BendEffect tremoloBar
    MixTableChange mixTableChange
    SlapEffect slapEffect = SlapEffect.NONE
    boolean vibrato = false
}

class BeatStroke {
    BeatStrokeDirection direction = BeatStrokeDirection.NONE
    int value = 0
    def swapDirection() {
        return switch(direction) {
            case BeatStrokeDirection.UP -> BeatStrokeDirection.DOWN
            case BeatStrokeDirection.DOWN -> BeatStrokeDirection.UP
            default -> direction
        }
    }
}

enum BeatStatus {
    EMPTY(0),
    NORMAL(1),
    REST(2),
    final int value
    BeatStatus(int value) { this.value = value }
    static BeatStatus from(int value) { values().find{ it.value == value } }
}

@TupleConstructor
abstract class HarmonicEffect {
    final int type
}

class NaturalHarmonic extends HarmonicEffect {
    NaturalHarmonic() { super(1) }
}

@TupleConstructor
class ArtificialHarmonic extends HarmonicEffect {
    Pitch pitch
    Octave octave
    ArtificialHarmonic() { super(2) }
}

@TupleConstructor
class TappedHarmonic extends HarmonicEffect {
    int fret
    TappedHarmonic() { super(3) }
}

class PinchHarmonic extends HarmonicEffect {
    PinchHarmonic() { super(4) }
}

class SemiHarmonic extends HarmonicEffect {
    SemiHarmonic() { super(5) }
}

class Pitch {
    enum Intonation {
        SHARP(1, 'C C# D D# E F F# G G# A A# B'.split()),
        FLAT(-1, 'C Db D Eb E F Gb G Ab A Bb B'.split())
        int accidental
        List<String> semitones
        Intonation(accidental, semitones) {
            this.accidental = accidental
            this.semitones = semitones
        }
        static from(int value) { values().find { it.accidental == value }}
    }
    int just
    int value
    Intonation intonation
    Pitch(int tone, int accidental) {
        this.just = tone % 12
        this.value = (this.just + accidental) % 12
        this.intonation = Intonation.from(accidental)
    }
    Pitch(int tone, Intonation intonation) {
        this.just = tone % 12
        this.intonation = intonation
        this.value = (this.just + intonation.accidental) % 12
    }
    @Override String toString() { intonation.semitones[value] }
}

@TupleConstructor
class MixTableItem {
    int value = 0
    int duration = 0
    boolean allTracks = false
}

@TupleConstructor
class WahEffect {
    private static final OFF = new WahEffect(-2)
    private static final NONE = new WahEffect(-1)
    int value = -1
    boolean display = false
    def isOff() { value == OFF.value }
    def isNone() { value == NONE.value }
    def isOn() { value in (0..100) }
}

/* All bend presets */
enum BendType {
    NONE(0),
    /* Bends */
    BEND(1),
    BEND_RELEASE(2),
    BEND_RELEASE_BEND(3),
    PREBEND(4),
    PREBEND_RELEASE(5),
    /* Tremolo Bar */
    DIP(6),
    DIVE(7),
    RELEASE_UP(8),
    INVERTED_DIP(9),
    RETURN_(10),
    RELEASE_DOWN(11)
    final int value
    BendType(int value) { this.value = value }
    static BendType from(int value) { values().find{ it.value == value }}
}

class MixTableChange {
    MixTableItem instrument
    RSEInstrument rse
    MixTableItem volume
    MixTableItem balance
    MixTableItem chorus
    MixTableItem reverb
    MixTableItem phaser
    MixTableItem tremolo
    String tempoName = ''
    MixTableItem tempo
    boolean hideTempo = true
    WahEffect wah
    boolean useRSE = false
}

enum GraceEffectTransition {
    NONE(0),
    SLIDE(1),
    BEND(2),
    HAMMER(3),
    final int value
    GraceEffectTransition(int value) { this.value = value }
    static GraceEffectTransition from(int value) { values().find{ it.value == value }}
}

class Velocities {
    static final int minVelocity = 15
    static final int velocityIncrement = 16
    static final int pianoPianissimo = minVelocity
    static final int pianissimo = minVelocity + velocityIncrement
    static final int piano = minVelocity + velocityIncrement * 2
    static final int mezzoPiano = minVelocity + velocityIncrement * 3
    static final int mezzoForte = minVelocity + velocityIncrement * 4
    static final int forte = minVelocity + velocityIncrement * 5
    static final int fortissimo = minVelocity + velocityIncrement * 6
    static final int forteFortissimo = minVelocity + velocityIncrement * 7
    static final int defaultVelocity = forte
}

@TupleConstructor
class BendPoint {
    int position = 0
    int value = 0
    boolean vibrato = false
    /**
     * Gets the exact time when the point needs to be played (MIDI).
     * @param duration the full duration of the effect.
     */
    int getTime(int duration) {
        return (duration * position / BendEffect.MAX_POSITION) as int
    }
}

class BendEffect {
    BendType type = BendType.NONE
    int value = 0
    List<BendPoint> points = []
    /** The note offset per bend point offset. */
    static final int SEMITONE_LENGTH = 1
    /** The max position of the bend points (x axis) */
    static final int MAX_POSITION = 12
    /** The max value of the bend points (y axis) */
    static final int MAX_VALUE = SEMITONE_LENGTH * 12
}
