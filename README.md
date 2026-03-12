# JMS with Spring Boot gotchas

## Tibco
Se branch `tibco`.

1. Konfa följande i [application.yml](src/main/resources/application.yml):
```yaml
tibco:
  ems:
    url: <tcp://dinserver:1234>
    username: <user>
    password: <password>
```
2. Skapa köer i Tibco mha Gems.
   1. En kö som är non-exclusive. En ny kö blir det som standard. 
   2. En kö som är exclusive. Skapa först en en standardkö, högerklicka på den och välja `queue properties` och sätt `isExclusive` till `true`.
3. Ändra i `MessageListenerService` så den lyssnar på dina köer.
4. Starta två instanser av applikationen på olika portar.
5. Skicka nio meddelanden (med id 1-9) till den icke-exklusiva kön (byt till ditt könamn):
```bash
url -X POST http://localhost:8080/messages/LM.UTV.PEVEST.NONEXCLUSIVE\?messages\=1\&messages\=2\&messages\=3\&messages\=4\&messages\=5\&messages\=6\&messages\=7\&messages\=8\&messages\=9
``` 
6. Notera att meddelandena sprids jämnt (mha round robin) mellan de två instanserna du startat.
7. Skicka nio meddelanden (med id 1-9) till den icke-exklusiva kön (byt till ditt könamn):
```bash
url -X POST http://localhost:8080/messages/LM.UTV.PEVEST.EXCLUSIVE\?messages\=1\&messages\=2\&messages\=3\&messages\=4\&messages\=5\&messages\=6\&messages\=7\&messages\=8\&messages\=9
``` 
8. Notera att meddelandena endast hämtas av en av de två instanserna du startat.

Om man vill göra lite överkurs i det senare exemplet kan man döda den instans som håller på att bearbeta ett meddelande och då ser man att meddelandet lämnas tillbaka till bussen och att den instansen som fortfarande lever börjar behandla meddelandena.