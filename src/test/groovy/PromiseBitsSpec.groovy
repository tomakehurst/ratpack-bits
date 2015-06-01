import ratpack.exec.Execution
import ratpack.exec.Promise
import ratpack.test.exec.ExecHarness
import spock.lang.Specification
import spock.lang.Timeout

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

class PromiseBitsSpec extends Specification {

    @Timeout(5)
    void 'future promise'() {
        expect:
            CompletableFuture<String> futureThing = new CompletableFuture<>()

            ExecHarness.harness().run { execControl ->
                execControl.exec().start { execution ->
                    execution.blocking {
                        println "Working in ${Thread.currentThread().name}"
                        Thread.sleep(2000)
                        return "This thing"
                    }.then { val ->
                        println val
                        futureThing.complete(val)
                    }
                }

                Thread.sleep(2500)
                println "Awaiting delivery in ${Thread.currentThread().name}"
                String thing = futureThing.get()
                println "Delivered"
                assert thing == 'This thing'
            }
    }

    @Timeout(5)
    void 'wrapper'() {
        expect:
            def result = ExecHarness.yieldSingle { execControl ->
                Future<String> futureThing = FutureWrapper.toFuture(execControl.promiseOf("Hello!"))
                def retrieved = futureThing.get()
            }
            result.value == "Hello!"

    }
}

class FutureWrapper {

    public static <T> Future<T> toFuture(Promise<T> promise) {
        CompletableFuture<T> futureThing = new CompletableFuture<>()
        Execution.current().controller.control.exec().start { execution ->
            promise.then { T val ->
                futureThing.complete(val)
            }
        }

        futureThing
    }
}

