package tabos.ui

import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.value.ChangeListener
import tabos.TabOS

class BindUtil {
    static def prop(Object o, String property) {
        new SimpleDoubleProperty(o[property] as double).tap {
            ChangeListener listener = (observer, oldVal, newVal) -> o[property] = newVal as double
            addListener listener
            TabOS.subscriptions.get(it, []) << listener
        }
    }
}
