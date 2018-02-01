package be.vdab;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class Bank {
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
    // Voegt een record toe in de tabel rekeningen
    public void maakNieuweRekening(Scanner scanner, Connection connection) throws SQLException, RekeningNummerException {
        long rekeningNummer = vraagRekeningNummer(scanner);
        if ( zoekRekening(connection, rekeningNummer) == null) {
            try (PreparedStatement statementInsert = connection.prepareStatement(INSERT_REKENING)) {
                statementInsert.setLong(1, rekeningNummer);
                statementInsert.executeUpdate();
                System.out.println("Nieuwe rekening gemaakt");
            }
        } else {
            System.out.println("Rekening bestaat reeds");
        }
    }
    // Vraagt de gebruiker om een rekeningnummer en retourneert dat als het een geldig nr is.
    private long vraagRekeningNummer(Scanner scanner) throws RekeningNummerException {
        System.out.print("Rekeningnummer (12 cijfers): ");
        long nummer = scanner.nextLong();
        if (! isGeldigRekeningNr(nummer)) {
            throw new RekeningNummerException();
        }
        return nummer;
    }
    // Controleert of het meegegeven nummer een geldig rekeningnummer is.
    private boolean isGeldigRekeningNr(long nummer) {
        int lengte = String.valueOf(nummer).length();
        if ( lengte != 12) {
            return false;
        }
        return ((nummer/100) % 97) == (nummer%100);
    }
    // Controleert of deze rekening al bestaat in de database.
    private Rekening zoekRekening(Connection connection, long rekeningNummer) throws SQLException {
        try (PreparedStatement statementSelectRekening = connection.prepareStatement(SELECT_REKENING)) {
            statementSelectRekening.setLong(1, rekeningNummer);
            try (ResultSet resultSet = statementSelectRekening.executeQuery()) {
                return resultSet.next()? new Rekening(resultSet.getLong("RekeningNr"), resultSet.getBigDecimal("Saldo")): null;
            }
        }
    }
    public void consulteerSaldo(Scanner scanner, Connection connection) throws RekeningNummerException, SQLException {
        long rekeningNummer = vraagRekeningNummer(scanner);
        try (PreparedStatement statementSelectSaldo = connection.prepareStatement(SELECT_SALDO)) {
            statementSelectSaldo.setLong(1, rekeningNummer);
            try (ResultSet resultSet = statementSelectSaldo.executeQuery()) {
                if(resultSet.next()) {
                    System.out.println("Saldo: " + resultSet.getLong("Saldo"));
                } else {
                    System.out.println("Rekening niet gevonden");
                }
            }
        }
    }
    public void maakOverschrijving(Scanner scanner, Connection connection) throws RekeningNummerException, SQLException {
        System.out.println("Van welke rekening wilt u overschrijven?");
        long rekeningNummerVan = vraagRekeningNummer(scanner);
        System.out.println("Naar welke rekening wilt u overschrijven?");
        long rekeningNummerNaar = vraagRekeningNummer(scanner);
        if (rekeningNummerVan != rekeningNummerNaar) {
            Rekening rekeningVan = zoekRekening(connection, rekeningNummerVan);
            Rekening rekeningNaar = zoekRekening(connection, rekeningNummerNaar);
            if(rekeningVan!=null && rekeningNaar!=null) {
                System.out.print("Bedrag: ");
                BigDecimal bedrag = scanner.nextBigDecimal();
                // Voer de overschrijving uit op de objecten
                if(rekeningVan.overschrijven(rekeningNaar, bedrag)){
                    // Voer de updates door in de database
                    try (PreparedStatement statementUpdateSaldo = connection.prepareStatement(UPDATE_SALDO)) {
                        // saldo op rekeningVan updaten
                        statementUpdateSaldo.setBigDecimal(1, rekeningVan.getSaldo());
                        statementUpdateSaldo.setLong(2, rekeningVan.getRekeningNr());
                        statementUpdateSaldo.addBatch();
                        // saldo op rekeningNaar updaten
                        statementUpdateSaldo.setBigDecimal(1, rekeningNaar.getSaldo());
                        statementUpdateSaldo.setLong(2, rekeningNaar.getRekeningNr());
                        statementUpdateSaldo.addBatch();
                        statementUpdateSaldo.executeBatch();
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
