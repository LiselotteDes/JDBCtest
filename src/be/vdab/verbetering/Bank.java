package be.vdab.verbetering;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Scanner;

public class Bank {
    private static final String URL = "jdbc:mysql://localhost/bank?useSSL=false";
    private static final String USER = "cursist";
    private static final String PASSWORD = "cursist";
    private static final String INSERT_REKENING = "insert into rekeningen(RekeningNr) values (?)";
    private static final String SELECT_REKENING = "select RekeningNr, Saldo from rekeningen where RekeningNr = ?";
    private static final String SELECT_SALDO = "select Saldo from rekeningen where RekeningNr = ?";
    private static final String UPDATE_SALDO = "update rekeningen set Saldo = ? where RekeningNr = ?";
    // Toont het keuzemenu en retourneert de keuze van de gebruiker
    public int maakKeuze(Scanner scanner) {
        int keuze = 0;
        System.out.println("Tik het getal van uw keuze:" +
                "\n1. Nieuwe rekening" +
                "\n2. Saldo consulteren" +
                "\n3. Overschrijven");
        keuze = scanner.nextInt();
        while(keuze < 1 || keuze > 3) {
            System.out.print("Geef een geldig keuzenummer (1, 2, 3): ");
            keuze = scanner.nextInt();
        }
        return keuze;
    }
    public void maakNieuweRekening(Scanner scanner) throws RekeningNummerException {
        long rekeningNummer = vraagRekeningNummer(scanner);
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
                PreparedStatement statementInsert = connection.prepareStatement(INSERT_REKENING)) {
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            connection.setAutoCommit(false);
            statementInsert.setLong(1, rekeningNummer);
            statementInsert.executeUpdate();
            System.out.println("Nieuwe rekening gemaakt");
            connection.commit();
        } catch (SQLException ex) {
            System.out.println("Rekening bestaat reeds");
        }
    }
    private long vraagRekeningNummer(Scanner scanner) throws RekeningNummerException {
        System.out.print("Rekeningnummer (12 cijfers): ");
        long nummer = scanner.nextLong();
        if (! isGeldigRekeningNr(nummer)) {
            throw new RekeningNummerException();
        }
        return nummer;
    }
    private boolean isGeldigRekeningNr(long nummer) {
        int lengte = String.valueOf(nummer).length();
        if ( lengte != 12) {
            return false;
        }
        return ((nummer/100) % 97) == (nummer%100);
    }
    private Rekening zoekRekening(long rekeningNummer) {
        Rekening rekening = null;
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
                PreparedStatement statementSelectRekening = connection.prepareStatement(SELECT_REKENING)) {
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            connection.setAutoCommit(false);
            statementSelectRekening.setLong(1, rekeningNummer);
            try (ResultSet resultSet = statementSelectRekening.executeQuery()) {
                rekening = resultSet.next()? new Rekening(resultSet.getLong("RekeningNr"), resultSet.getBigDecimal("Saldo")): null;
            }
            connection.commit();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return rekening;
    }
    public void consulteerSaldo(Scanner scanner) throws RekeningNummerException {
        long rekeningNummer = vraagRekeningNummer(scanner);
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
                PreparedStatement statementSelectSaldo = connection.prepareStatement(SELECT_SALDO)) {
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            connection.setAutoCommit(false);
            statementSelectSaldo.setLong(1, rekeningNummer);
            try (ResultSet resultSet = statementSelectSaldo.executeQuery()) {
                if(resultSet.next()) {
                    System.out.println("Saldo: " + resultSet.getLong("Saldo"));
                } else {
                    System.out.println("Rekening niet gevonden");
                }
            }
            connection.commit();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
    public void maakOverschrijving(Scanner scanner) throws RekeningNummerException {
        System.out.println("Van welke rekening wilt u overschrijven?");
        long rekeningNummerVan = vraagRekeningNummer(scanner);
        System.out.println("Naar welke rekening wilt u overschrijven?");
        long rekeningNummerNaar = vraagRekeningNummer(scanner);
        if (rekeningNummerVan != rekeningNummerNaar) {
            Rekening rekeningVan = zoekRekening(rekeningNummerVan);
            Rekening rekeningNaar = zoekRekening(rekeningNummerNaar);
            if(rekeningVan!=null && rekeningNaar!=null) {
                System.out.print("Bedrag: ");
                BigDecimal bedrag = scanner.nextBigDecimal();
                // Voer de overschrijving uit op de objecten
                if(rekeningVan.overschrijven(rekeningNaar, bedrag)){
                    // Voer de updates door in de database
                    try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
                            PreparedStatement statementUpdateSaldo = connection.prepareStatement(UPDATE_SALDO)) {
                        connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
                        connection.setAutoCommit(false);
                        // saldo op rekeningVan updaten
                        statementUpdateSaldo.setBigDecimal(1, rekeningVan.getSaldo());
                        statementUpdateSaldo.setLong(2, rekeningVan.getRekeningNr());
                        statementUpdateSaldo.addBatch();
                        // saldo op rekeningNaar updaten
                        statementUpdateSaldo.setBigDecimal(1, rekeningNaar.getSaldo());
                        statementUpdateSaldo.setLong(2, rekeningNaar.getRekeningNr());
                        statementUpdateSaldo.addBatch();
                        int[] aantalAangepasteRecords = statementUpdateSaldo.executeBatch();
                        int totaalAangepast = Arrays.stream(aantalAangepasteRecords).sum();
                        /*
                        Deze controle is zeker nodig als het isolation level op read_committed staat,
                        want er kan ondertussen één van de rekeningen verwijderd zijn.
                        Nu staat het isolation level op serializable, dus deze controle zal moeten kloppen,
                        anders zou er iets zeer vreemds misgelopen zijn.
                        Dit mag toch blijven staan in de code.
                        */
                        if (totaalAangepast == 2) {
                            connection.commit();
                        } else {
                            connection.rollback();
                        }
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                } else {
                    System.out.println("Saldo niet voldoende");
                }
            } else {
                System.out.println("Rekening(en) niet gevonden");
            }
        } else {
            System.out.println("Twee keer hetzelfde rekeningnummer.");
        }
        
    }
}