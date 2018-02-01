package be.vdab;
import java.math.BigDecimal;

public class Rekening {
    private long rekeningNr;
    private BigDecimal saldo = BigDecimal.ZERO;
    
    public Rekening(long rekeningNr) {
        this.rekeningNr = rekeningNr;
    }
    public Rekening(long rekeningNr, BigDecimal saldo) {
        this.rekeningNr = rekeningNr;
        this.saldo = saldo;
    }
    public long getRekeningNr() {
        return rekeningNr;
    }
    public BigDecimal getSaldo() {
        return saldo;
    }
    public void storten(BigDecimal bedrag) {
        saldo = saldo.add(bedrag);
    }
    public boolean overschrijven(Rekening rekening, BigDecimal bedrag) {
        if(saldo.compareTo(bedrag) >= 0) {
            rekening.storten(bedrag);
            saldo = saldo.subtract(bedrag);
            return true;
        } else {
            return false;
        }
    }
    @Override
    public String toString() {
        return rekeningNr + ": " + saldo + " euro";
    }
}
