package dog.del.app.markdown.utils

import com.vladsch.flexmark.util.data.DataKey
import com.vladsch.flexmark.util.data.MutableDataSet

inline fun mutableDataSetOf(vararg entries: Pair<DataKey<*>, *>): MutableDataSet = MutableDataSet().apply {
    for (entry in entries) {
        set(entry.first, entry.second)
    }
}