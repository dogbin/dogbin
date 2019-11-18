package dog.del.app.utils

import org.koin.core.context.GlobalContext
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier

/**
 * Get a Koin instance
 * @param qualifier
 * @param scope
 * @param parameters
 */
@JvmOverloads
inline fun <reified T> get(
    qualifier: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
): T = GlobalContext.get().koin.get(qualifier, parameters)