package philosophers

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Semaphore

import org.apache.zookeeper._

import scala.util.Random

case class Philosopher(id: Int,
                       hostPort: String,
                       root: String,
                       left: Semaphore,
                       right: Semaphore,
                       seats: Integer) extends Watcher {

  val zk = new ZooKeeper(hostPort, 3000, this)
  val mutex = new Object()
  val path: String = root + "/" + id.toString

  def getTime(): String = {
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS").format(LocalDateTime.now)
  }

  override def process(event: WatchedEvent): Unit = {
    mutex.synchronized {
      mutex.notify()
    }
  }

  def eat(): Unit = {
    printf("%s Philosopher %d preparing to eat\n", getTime(), id)
    mutex.synchronized {
      while (true) {
        if (zk.exists(path, false) == null) {
          zk.create(path, Array.emptyByteArray, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL)
        }
        val active = zk.getChildren(root, this)
        if (active.size() > seats) {
          zk.delete(path, -1)
          mutex.wait(3000)
          Thread.sleep(Random.nextInt(5) * 100)
        } else {
          left.acquire()
          printf("%s Philosopher %d took left fork\n", getTime(), id)
          right.acquire()
          printf("%s Philosopher %d took right fork\n", getTime(), id)
          Thread.sleep((Random.nextInt(3) + 1) * 1000)
          left.release()
          printf("%s Philosopher %d placed left fork back\n", getTime(), id)
          right.release()
          printf("%s Philosopher %d placed right fork back\n", getTime(), id)
          return
        }
      }
    }
  }

  def think(): Unit = {
    printf("%s Philosopher %d thinks\n", getTime(), id)
    zk.delete(path, -1)
    Thread.sleep((Random.nextInt(3) + 1) * 1000)
    printf("%s Philosopher %d thought enough\n", getTime(), id)
  }
}