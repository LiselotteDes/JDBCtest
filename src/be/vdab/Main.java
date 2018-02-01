package be.vdab;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Scanner;

public class Main {
    private static final String URL = "jdbc:mysql://localhost/bank?useSSL=false";
    private static final String USER = "cursist";
    private static final String PASSWORD = "cursist";
    
    public static void main(String[] args) {
        Bank bank = new Bank();
        try (Scanner scanner = new Scanner(System.in)) {
            try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
                connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                connection.setAutoCommit(false);
                switch(bank.maakKeuze(scanner)) {
                    case 1: 
                        bank.maakNieuweRekening(scanner, connection);
                        break;
                    case 2: 
                        bank.consulteerSaldo(scanner, connection);
                        break;
                    case 3: 
                        bank.maakOverschrijving(scanner, connection);
                        break;
                    default: 
                        System.out.println("Verkeerde invoer");
                        break;
                }
                connection.commit();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            catch (RekeningNummerException ex) {
                System.out.println("Ongeldig rekeningNummer");
            }
        }
    }
    
}
