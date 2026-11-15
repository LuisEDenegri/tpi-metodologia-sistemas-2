package pedidos.pruebas;

/**
 * Runner de pruebas minimo. No se usa JUnit porque el trabajo pide no depender
 * de librerias externas y porque un runner de 60 lineas alcanza para lo que hay
 * que verificar.
 *
 * ponytail: techo deliberado. Si el proyecto crece a decenas de casos o necesita
 * ejecucion selectiva, reportes o integracion con CI, migrar a JUnit 5.
 */
public class Verificador {

    private int pasadas = 0;
    private int fallidas = 0;
    private final String titulo;

    public Verificador(String titulo) {
        this.titulo = titulo;
        System.out.println();
        System.out.println("=========================================================");
        System.out.println(titulo);
        System.out.println("=========================================================");
    }

    public void igual(String caso, Object esperado, Object obtenido) {
        boolean ok = esperado == null ? obtenido == null : esperado.equals(obtenido);
        registrar(ok, caso, String.valueOf(esperado), String.valueOf(obtenido));
    }

    public void igual(String caso, double esperado, double obtenido) {
        boolean ok = Math.abs(esperado - obtenido) < 0.0001;
        registrar(ok, caso, String.valueOf(esperado), String.valueOf(obtenido));
    }

    public void cierto(String caso, boolean condicion) {
        registrar(condicion, caso, "true", String.valueOf(condicion));
    }

    public void falso(String caso, boolean condicion) {
        registrar(!condicion, caso, "false", String.valueOf(condicion));
    }

    private void registrar(boolean ok, String caso, String esperado, String obtenido) {
        if (ok) {
            pasadas++;
            System.out.println("  [OK]    " + caso);
        } else {
            fallidas++;
            System.out.println("  [FALLA] " + caso);
            System.out.println("          esperado: " + esperado);
            System.out.println("          obtenido: " + obtenido);
        }
    }

    public boolean resumen() {
        System.out.println("  ---------------------------------------------------");
        System.out.println("  " + titulo + ": " + pasadas + " pasadas, " + fallidas + " fallidas");
        return fallidas == 0;
    }

    public int fallidas() {
        return fallidas;
    }
}
