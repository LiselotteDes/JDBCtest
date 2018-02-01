package be.vdab.verbetering;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Bank bank = new Bank();
        try (Scanner scanner = new Scanner(System.in)) {
            switch(bank.maakKeuze(scanner)) {
                case 1: 
                    bank.maakNieuweRekening(scanner);
                    break;
                case 2: 
                    bank.consulteerSaldo(scanner);
                    break;
                case 3: 
                    bank.maakOverschrijving(scanner);
                    break;
                default: 
                    System.out.println("Verkeerde invoer");
                    break;
            }
        } catch (RekeningNummerException ex) {
            System.out.println("Ongeldig rekeningNummer");
        }
    }
    
}
