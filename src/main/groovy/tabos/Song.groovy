package tabos

class Song {

}

enum SlideType {
    INTO_FROM_ABOVE(-2),
}

enum Fingering {
    OPEN(-1),
    THUMB(0),
    INDEX(1),
    MIDDLE(2),
    ANNULAR(3),
    LITTLE(4),

    int value

    Fingering(int value) {
        this.value = value
    }
}