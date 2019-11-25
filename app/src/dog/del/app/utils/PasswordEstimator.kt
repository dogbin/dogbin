package dog.del.app.utils

import me.gosimple.nbvcxz.Nbvcxz
import me.gosimple.nbvcxz.resources.ConfigurationBuilder
import me.gosimple.nbvcxz.resources.DictionaryBuilder

object PasswordEstimator {
    fun init() = Nbvcxz(
        ConfigurationBuilder()
            .setDictionaries(
                ConfigurationBuilder.getDefaultDictionaries() + DictionaryBuilder().setDictionaryName("dogbin")
                    .setExclusion(true)
                    .addWord("dogbin", 0)
                    .addWord("pastebin", 0)
                    .addWord("deletescape", 0)
                    .addWord("hastebin", 0)
                    .createDictionary()
            )
            .createConfiguration()
    )
}