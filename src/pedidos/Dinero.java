package pedidos;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * Importe monetario.
 *
 * REFACTORING: Replace Primitive Obsession with Value Object (Fowler).
 *
 * Antes el dinero era `double`. Eso traia dos problemas distintos:
 *
 *   1. PRECISION. El double es binario y no puede representar exactamente
 *      valores decimales como 0.10. Sumar 1234.10 diez veces no daba 12341.0
 *      sino 12340.999999999998. Sobre miles de pedidos, la facturacion
 *      acumulada no cierra contra contabilidad.
 *
 *   2. FORMATO. "$" + double delega en Double.toString(), que pasa a notacion
 *      cientifica desde 10^7. El reporte para administracion mostraba $1.7E7.
 *
 * Internamente usa BigDecimal con 2 decimales y redondeo HALF_UP, que es el
 * redondeo comercial habitual. La clase es inmutable: toda operacion devuelve
 * un Dinero nuevo, asi que no hay forma de que un importe cambie a espaldas de
 * quien lo tiene.
 */
public final class Dinero implements Comparable<Dinero> {

    // Estas dos constantes van declaradas ANTES que CERO y no al reves: los
    // inicializadores estaticos corren en orden textual, y el constructor de
    // Dinero usa REDONDEO. Si CERO se declara primero, se construye cuando
    // REDONDEO todavia vale null y la clase falla al cargarse con un
    // ExceptionInInitializerError.
    private static final int DECIMALES = 2;
    private static final RoundingMode REDONDEO = RoundingMode.HALF_UP;

    public static final Dinero CERO = new Dinero(BigDecimal.ZERO);

    private final BigDecimal monto;

    private Dinero(BigDecimal monto) {
        this.monto = monto.setScale(DECIMALES, REDONDEO);
    }

    public static Dinero de(String monto) {
        return new Dinero(new BigDecimal(monto));
    }

    public static Dinero de(long monto) {
        return new Dinero(BigDecimal.valueOf(monto));
    }

    public Dinero mas(Dinero otro) {
        return new Dinero(this.monto.add(otro.monto));
    }

    public Dinero por(int veces) {
        return new Dinero(this.monto.multiply(BigDecimal.valueOf(veces)));
    }

    /** Aplica un porcentaje. porcentaje(50) sobre 4500 devuelve 2250. */
    public Dinero porcentaje(int porcentaje) {
        return new Dinero(this.monto
                .multiply(BigDecimal.valueOf(porcentaje))
                .divide(BigDecimal.valueOf(100), DECIMALES, REDONDEO));
    }

    public boolean esMayorOIgualA(Dinero otro) {
        return this.monto.compareTo(otro.monto) >= 0;
    }

    public boolean esNegativo() {
        return this.monto.signum() < 0;
    }

    public boolean esCero() {
        return this.monto.signum() == 0;
    }

    /** Valor numerico. Solo para interoperar con codigo que todavia usa double. */
    public double valor() {
        return monto.doubleValue();
    }

    /** Formato argentino: punto para miles, coma para decimales. */
    @Override
    public String toString() {
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
        simbolos.setGroupingSeparator('.');
        simbolos.setDecimalSeparator(',');
        return new DecimalFormat("#,##0.00", simbolos).format(monto);
    }

    @Override
    public int compareTo(Dinero otro) {
        return this.monto.compareTo(otro.monto);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Dinero)) {
            return false;
        }
        return this.monto.compareTo(((Dinero) obj).monto) == 0;
    }

    @Override
    public int hashCode() {
        return monto.stripTrailingZeros().hashCode();
    }
}
