# Bankomatkarten Infos

***
***
***
## ***Update 8.1.2014:***<br>
Aufgrund des aktuellen Medieninteresses an dem Thema hier eine kleine Stellungnahme:

***Zur Klarstellung:*** ich wollte da nichts aufdecken oder hacken. Ich hab mich nur rein interessehalber mit dem Thema beschäftigt, weil ich prinzipiell das Thema NFC sehr spannend finde und einfach mehr darüber lernen wollte.

Ich habe auch nichts "gehackt" oder "geknackt". Alles was ich in der App gemacht habe ist offen im EMV Zahlungsstandard beschrieben. Alle notwendigen Infos dazu kann jeder technisch Interessierte frei zugänglich ganz offiziell hier in den Spezifikationen nachlesen: [http://www.emvco.com/specifications.aspx?id=223](http://www.emvco.com/specifications.aspx?id=223)

Außerdem ist es keineswegs neu, dass die Bankomatkarten diese Daten beinhalten. Genau dieselben Info kann man auch aus jeder alten Bankomatkarte (ohne NFC) auslesen. Man muss sich nur um ein paar Euro ein Smartcard-Lesegerät kaufen und eine entsprechende Applkation bauen. Das haben andere bereits vor mir gezeigt.

Einzig neu ist die Tatsache, dass diese Infos jetzt auch drahtlos per NFC lesbar sind.
***
***
***


### Android NFC-App zum Auslesen von österreichischen Bankomatkarten bzw. Maestro Karten
english version below..<br>

Sollte auf Android 4.0.3 oder höher funktionieren und benötigt ein Gerät mit NFC. 


### Links
Hier das Google+ Posting mit dem der Spaß begann: [Fun with NFC.. :-)](https://plus.google.com/100041258817977286971/posts/jZUhAhRUpqL)

### Danke
Vielen Dank an das Projekt [javaemvreader](https://code.google.com/p/javaemvreader/) von dem ich einige Klassen zum Dekodieren der EMV Daten übernommen habe (released unter Apache 2.0 license). 

### Was ist das? ##

Diese App versucht die letzten x Transaktionen sowie einige andere Infos aus einer NFC-aktivierten Bankomatkarte via NFC auszulesen. Es kann durchaus sein, dass es nicht mit allen Karten funktioniert. Getestet bisher nur mit einer Bank Austria Bankomatkarte.

### Screenshots

![Allgemeine Infos zur Karte](https://raw2.github.com/johnzweng/bankomatinfos/master/doc/sreenshots/result_tab_infos_256px.png)
![Liste der letzten Transaktionen](https://raw2.github.com/johnzweng/bankomatinfos/master/doc/sreenshots/result_tab_transactions_256px.png)
![Detailliertes Log](https://raw2.github.com/johnzweng/bankomatinfos/master/doc/sreenshots/result_tab_log_256px.png)


### Details

Die App versucht eine SmartCard via NFC zu lesen. Dazu prüft sie ob entweder die Paylife Quick Applikation (`AID D040000001000002`) bzw. die Maestro (Bankomat) Applikation (`AID A0000000043060`) auf der Karte vorhanden ist. Falls ja, wird versucht einige frei zugängliche EFs (elementary files) auszulesen (es erfolgt keine Authentifizierung).

### Wo findet man weitere Infos:

- [Offizielle EMV Spezifikationen](http://www.emvco.com/specifications.aspx?id=223)
- [Guter Einführungstalk wie EMV Transaktionen ablaufen am 29C3](https://www.youtube.com/watch?v=qqobg1-HrfY)
- [EMVCo FAQ](http://www.emvco.com/faq.aspx?id=37)
- [EMV Ressourcen auf smartcardalliance.org](http://www.smartcardalliance.org/pages/publications-emv-faq)
- Wikipedia über [EMV](https://en.wikipedia.org/wiki/EMV)
- [EMV Tutorial von opemnSCDP.org](http://www.openscdp.org/scripts/tutorial/emv/reademv.html)
- Sehr gute undd komplette [Auflistung und Erklärung von EMV Tags](http://www.eftlab.co.uk/index.php/site-map/knowledge-base/145-emv-nfc-tags)
- [Deutsschsprachiges PDF über das BER-TLV Codierungs-Schema welches auf EMV SmartCards genutzt wird](http://koepferl.eu/publikationen/TLV.pdf)
- Blog-Artikel (english): [Getting information from an EMV chip card with Java](http://blog.saush.com/2006/09/08/getting-information-from-an-emv-chip-card/)
- [Cardwerk's excellent online resources on ISO 7816-4 SmartCards](http://www.cardwerk.com/smartcards/smartcard_standard_ISO7816-4_6_basic_interindustry_commands.aspx)
- Wikipedia über [ISO 7816-4 Smart Card APDU](https://en.wikipedia.org/wiki/Smart_card_application_protocol_data_unit)
- [Masterarbeit von Michael Schouwenaar mit zahlreichen interessanten Infos zu EMV (englisch)](http://www.ru.nl/publish/pages/578936/emv-cards_and_internet_banking_-_michael_schouwenaar.pdf) 
- Eine andere interessante [Bachelorarbeit von Christian Mäder und Sandro Vogler mit zahlreichen nützlichen Hintergrundinfos zu EMV (auf deutsch)](http://eprints.hsr.ch/309/1/Bachelor_Thesis_Maeder_Vogler.pdf)


<br>
<br>
****
****
<br>


### Android NFC-App for reading some infos from Austrian Bankomat Cards (Maestro banking cards). 

Should work on Android 4.0.3 and above.
Needs a device with NFC support. 

### Links
Here the Google+ Posting (in german) where the fun began: [Fun with NFC.. :-)](https://plus.google.com/100041258817977286971/posts/jZUhAhRUpqL)

### Thanks
Many thanks to the project [javaemvreader](https://code.google.com/p/javaemvreader/) from which I borrowed some classes for decoding EMV data (released under Apache 2.0 License). 


### What's this ##

This app tries to read the last transactions and some general infos from a NFC-enabled Austrian Bankomatkarte (Maestro debit card) via NFC. It may not work on all cards, only tested with cards from Bank Austria for now.

### In detail

This android app tries to read a SmartCard via NFC. It checks if the card contains the Paylife Quick application (`AID D040000001000002`) and the Maestro (Bankomat) application (`AID A0000000043060`) and tries to read some free accessible files (no authentication is performed to card).


### Where to look for further info:

- [Official EMV Specifications](http://www.emvco.com/specifications.aspx?id=223)
- [Good overview talk how EMV Transaktionen work at 29C3](https://www.youtube.com/watch?v=qqobg1-HrfY)
- [EMVCo FAQ](http://www.emvco.com/faq.aspx?id=37)
- [EMV resources at smartcardalliance.org](http://www.smartcardalliance.org/pages/publications-emv-faq)
- Wikipedia on [EMV](https://en.wikipedia.org/wiki/EMV)
- [EMV Tutorial from opemnSCDP.org](http://www.openscdp.org/scripts/tutorial/emv/reademv.html)
- Very good and complete [list of EMV Tags](http://www.eftlab.co.uk/index.php/site-map/knowledge-base/145-emv-nfc-tags)
- [German PDF on the BER-TLV encoding sheme used on EMV cards](http://koepferl.eu/publikationen/TLV.pdf)
- Blog article [Getting information from an EMV chip card with Java](http://blog.saush.com/2006/09/08/getting-information-from-an-emv-chip-card/)
- [Cardwerk's excellent online resources on ISO 7816-4 SmartCards](http://www.cardwerk.com/smartcards/smartcard_standard_ISO7816-4_6_basic_interindustry_commands.aspx)
- Wikipedia on [ISO 7816-4 Smart Card APDU](https://en.wikipedia.org/wiki/Smart_card_application_protocol_data_unit)
- [Master thesis of Michael Schouwenaar with lots of interesting EMV infos](http://www.ru.nl/publish/pages/578936/emv-cards_and_internet_banking_-_michael_schouwenaar.pdf) 
- Another interesting [bachelor thesis by Christian Mäder and Sandro	Vogler with lots of background infos on EMV (in german)](http://eprints.hsr.ch/309/1/Bachelor_Thesis_Maeder_Vogler.pdf)

