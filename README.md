# JMS with Spring Boot gotchas

`MessageSenderService.send(msg: String)` sends `msg` to a JMS queue and save `msg` to a Postgres database. This operation has to be **atomic**.

**If `msg` contains "Foo" a RuntimeException is thrown!** 

## Test
1. Run the application. It starts on port 8080 and it also starts a Postgres database and an ActiveMQ broker.
2. Execute a POST to the application with message `Boo`:
```bash
curl -X POST http://localhost:8080/messages/Boo
```
3. In the database table `messages` the message `Boo` is stored.
4. Open up the [admin-GUI for ActiveMQ](http://localhost:8161/admin/queues.jsp) and you'll see that `demo.queue` contains the message `Boo`.
5. Now, Execute a POST to the application with message `Foo`:
```bash
curl -X POST http://localhost:8080/messages/Foo
```
6. You get a 500 as HTTP response code. `Foo` should not be stored in the database nor in `demo.queue`.
```sql
SELECT * FROM messages;
```
You'll see `Boo` but not `Foo`.  
In ActiveMQ you'll find both `Boo` and `Foo`.


7. Annotate `send` with `@Transactional`, restart the application and do another POST with message `Foo`.
In ActiveMQ you'll find another `Foo`.


8. Goto to [application.yml](src/main/resources/application.yml) and "comment in":
```yaml
  jms:
    template:
      session:
        transacted: true
```
9. Restart the application and do another POST with message `Foo`.
No new `Foo` in the database and no new `Foo` in ActiveMQ - woohoo!

When you set `spring.jms.template.session.transacted: true` Spring delays the JMS-send until the transaction commits. If it fails (rollback) the message is not sent. 
