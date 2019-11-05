package dog.del.app.utils

import kotlinx.coroutines.*
import kotlinx.coroutines.future.asCompletableFuture
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

inline fun CoroutineDispatcher.asExecutorService(): ExecutorService = DispatcherExcecutorService(this)

class DispatcherExcecutorService(private val dispatcher: CoroutineDispatcher) : ExecutorService {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(dispatcher + job)

    override fun shutdown() {
        runBlocking {
            job.cancelAndJoin()
        }
    }

    override fun <T : Any?> submit(task: Callable<T>): Future<T> = scope.async {
        task.call()
    }.asCompletableFuture()

    override fun <T : Any?> submit(task: Runnable, result: T): Future<T> = scope.async {
        task.run()
        result
    }.asCompletableFuture()

    override fun submit(task: Runnable): Future<*> = scope.async {
        task.run()
    }.asCompletableFuture()

    override fun shutdownNow(): MutableList<Runnable> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isShutdown(): Boolean = job.isCompleted

    override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T : Any?> invokeAny(tasks: MutableCollection<out Callable<T>>): T {
        TODO("Implement using channels")
    }

    override fun <T : Any?> invokeAny(tasks: MutableCollection<out Callable<T>>, timeout: Long, unit: TimeUnit): T {
        TODO("Implement using channels")
    }

    override fun isTerminated(): Boolean = job.isCompleted

    override fun <T : Any?> invokeAll(tasks: MutableCollection<out Callable<T>>): List<Future<T>> = tasks.map {
        scope.async { it.call() }.asCompletableFuture()
    }

    // TODO: does the timeout have to apply per task or overall?
    override fun <T : Any?> invokeAll(
        tasks: MutableCollection<out Callable<T>>,
        timeout: Long,
        unit: TimeUnit
    ): List<Future<T>> = tasks.map {
        scope.async {
            withTimeout(unit.toMillis(timeout)) {
                it.call()
            }
        }.asCompletableFuture()
    }


    override fun execute(task: Runnable) {
        dispatcher.dispatch(scope.coroutineContext, task)
    }

}