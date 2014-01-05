# Bankomatkarten Infos
***
english version below..

### Android NFC-App zum Auslesen von österreichischen Bankomatkarten bzw. Maestro Karten

Sollte auf Android 4.0.3 oder höher funktionieren und benötigt ein Gerät mit NFC. 


### Links
Hier das Google+ Posting mit dem der Spaß begann: [Fun with NFC.. :-)](https://plus.google.com/100041258817977286971/posts/jZUhAhRUpqL)

### Danke
Vielen Dank an das Projekt [javaemvreader](https://code.google.com/p/javaemvreader/) von dem ich einige Klassen zum Dekodieren der EMV Daten übernommen habe (released unter Apache 2.0 license). 

### Was ist das? ##

Diese App versucht die letzten x Transaktionen sowie einige andere Infos aus einer NFC-aktivierten Bankomatkarte via NFC auszulesen. Es kann durchaus sein, dass es nicht mit allen Karten funktioniert. Getestet bisher nur mit einer Bank Austria Bankomatkarte.

### Screenshots

![Algmeine Infos zur Karte](https://github.com/johnzweng/bankomatinfos/blob/master/sreenshots/result_tab_infos_256px.png?raw=true)
![Liste der letzten Transaktionen](https://github.com/johnzweng/bankomatinfos/blob/master/sreenshots/result_tab_transactions_256px.png?raw=true)
![Detailliertes Log](https://github.com/johnzweng/bankomatinfos/blob/master/sreenshots/result_tab_log_256px.png?raw=true)


### Details

Die App versucht eine SmartCard via NFC zu lesen. Dazu prüft sie ob entweder die Paylife Quick Applikation (`AID D040000001000002`) bzw. die Maestro (Bankomat) Applikation (`AID A0000000043060`) auf der Karte vorhanden ist. Falls ja, wird versucht einige frei zugängliche EFs (elementary files) auszulesen (es erfolgt keine Authentifizierung).

### Wo findet man weitere Infos:

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

- Wikipedia on [EMV](https://en.wikipedia.org/wiki/EMV)
- [EMV Tutorial from opemnSCDP.org](http://www.openscdp.org/scripts/tutorial/emv/reademv.html)
- Very good and complete [list of EMV Tags](http://www.eftlab.co.uk/index.php/site-map/knowledge-base/145-emv-nfc-tags)
- [German PDF on the BER-TLV encoding sheme used on EMV cards](http://koepferl.eu/publikationen/TLV.pdf)
- Blog article [Getting information from an EMV chip card with Java](http://blog.saush.com/2006/09/08/getting-information-from-an-emv-chip-card/)
- [Cardwerk's excellent online resources on ISO 7816-4 SmartCards](http://www.cardwerk.com/smartcards/smartcard_standard_ISO7816-4_6_basic_interindustry_commands.aspx)
- Wikipedia on [ISO 7816-4 Smart Card APDU](https://en.wikipedia.org/wiki/Smart_card_application_protocol_data_unit)
- [Master thesis of Michael Schouwenaar with lots of interesting EMV infos](http://www.ru.nl/publish/pages/578936/emv-cards_and_internet_banking_-_michael_schouwenaar.pdf) 
- Another interesting [bachelor thesis by Christian Mäder and Sandro	Vogler with lots of background infos on EMV (in german)](http://eprints.hsr.ch/309/1/Bachelor_Thesis_Maeder_Vogler.pdf)


