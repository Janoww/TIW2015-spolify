# Specifica

Un’applicazione Web consente di creare un rapporto mensile delle ore lavorative giornaliere spese nei progetti. L’amministratore accede tramite login a una pagina (HomeAdmin) in cui crea i progetti attivi; da qui accede a una pagina (GestioneProgetti) dove sceglie un progetto dall’elenco e gli associa gli utenti che ci lavorano, indicando il numero massimo di ore per utente e per progetto. Un utente lavoratore accede mediante login a una pagina (HomeUtente) in cui sceglie il progetto e inserisce le ore di lavoro spese in una certa data; nella stessa pagina può inoltre interrogare l’applicazione per visualizzare il numero di ore rendicontate finora e verificare di non aver superato il massimo impostato dall’amministratore

## Application requirements analysis

Un’applicazione Web consente di creare un rapporto mensile delle ore lavorative giornaliere spese nei progetti. L’amministratore accede tramite login a una pagina (HomeAdmin) in cui crea i progetti attivi; da qui accede a una pagina (GestioneProgetti) dove sceglie un progetto dall’elenco e gli associa gli utenti che ci lavorano, indicando il numero massimo di ore per utente e per progetto. Un utente lavoratore accede mediante login a una pagina (HomeUtente) in cui sceglie il progetto e inserisce le ore di lavoro spese in una certa data; nella stessa pagina può inoltre interrogare l’applicazione per visualizzare il numero di ore rendicontate finora e verificare di non aver superato il massimo impostato dall’amministratore

Pages (views), view components, events, actions

## Completamento delle specifiche

- L'applicazione gestisce solo le ore di un mese (per semplicità). Non si controlla la validità del numero intero del giorno inserito dall'utente rispetto al mese corrente.
- Esiste una pagina di default contenente la form di login visibile a tutti
- La pagina HomeAdmin contiene una form per creare progetti
- La pagina GestioneProgetti dell'admin contiene un elenco dei progetti dell'admin e un elenco dei lavoratori non ancora associati. Selezionando un progetto dall'elenco, cambia l'elenco dei lavoratori; selezionando un lavoratore, si crea l'assegnamento al progetto selezionato in precedenza.
- La pagina HomeUtente contiene una form in cui l'utente sceglie il progetto e inserisce data (come intero da 1 a 31) e numero di ore.
- L'interrogazione del numero di ore lavorate (nel mese) può essere svolta mostrando le ore lavorate e massime nella stessa pagina con cui il worker sceglie il progetto per cui rendicontare le ore (per esempio nell'elenco dei progetti associati al worker)
- Deve essere possibile fare logout e tornare alla home page pubblica con la form di login.
