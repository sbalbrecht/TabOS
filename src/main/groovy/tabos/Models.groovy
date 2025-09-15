package tabos

import groovy.transform.TupleConstructor

class PageSetup {
    @TupleConstructor
    class HeaderElement {
        String template = ''
        boolean visible = true
    }
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

@TupleConstructor
class RSEEqualizer {
    List<Float> knobs = []
    Float gain = 0.0f
}

class RSEInstrument {
    int instrument = -1
    int unknown = -1
    int soundBank = -1
    int effectNumber = -1
    String effectCategory = ''
    String effect = ''
}

class TrackRSE {
    RSEInstrument instrument
    RSEEqualizer equalizer
    int humanize = 0
    Accentuation autoAccentuation = Accentuation.NONE
//    def __attrs_post_init__(self)
//        if not self. equalizer.knobs:
//        self.equalizer.knobs = [ 0.0 ] * 3
}

enum TripletFeel {
    NONE(0),
    EIGHTH(1),
    SIXTEENTH(2),
    final int value
    TripletFeel(int value) { this.value = value }
    static from(int value) { values().find{ it.value == value } }
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
    static int minVelocity = 15
    static int velocityIncrement = 16
    static int pianoPianissimo = minVelocity
    static int pianissimo = minVelocity + velocityIncrement
    static int piano = minVelocity + velocityIncrement * 2
    static int mezzoPiano = minVelocity + velocityIncrement * 3
    static int mezzoForte = minVelocity + velocityIncrement * 4
    static int forte = minVelocity + velocityIncrement * 5
    static int fortissimo = minVelocity + velocityIncrement * 6
    static int forteFortissimo = minVelocity + velocityIncrement * 7
    static int defaultVelocity = forte
}

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
    static final int maxValue = SEMITONE_LENGTH * 12
}
