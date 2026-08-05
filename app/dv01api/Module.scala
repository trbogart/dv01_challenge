package dv01api

import com.google.inject.AbstractModule
import dv01api.service.LoanDataLoader

/** Forces [[LoanDataLoader]] to be constructed (and its CSV load to run) at app startup (except in dev mode). */
class Module extends AbstractModule:
  override def configure(): Unit =
    bind(classOf[LoanDataLoader]).asEagerSingleton()
