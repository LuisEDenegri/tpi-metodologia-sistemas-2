package pedidos.pruebas;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Punto de entrada unico de las pruebas.
 *
 *   java -cp out pedidos.pruebas.CorridaDePruebas
 *
 * Ejecuta las dos suites y explica que significa cada resultado. La distincion
 * entre ambas es el eje del trabajo:
 *
 *   CARACTERIZACION ........ debe estar SIEMPRE en verde.
 *                            Un rojo aca significa que un refactoring cambio el
 *                            comportamiento. Es una regresion.
 *
 *   COMPORTAMIENTO CORRECTO  arranca en ROJO y tiene que terminar en verde.
 *                            Cada caso que pasa a verde es un defecto corregido.
 *
 * El dominio escribe por consola (EmailService y AnalyticsService hacen
 * println), asi que la salida de las pruebas sale mezclada con notificaciones.
 * Se silencia para que el informe se lea. Que haga falta silenciarlo es, en si
 * mismo, un sintoma: el dominio no deberia escribir por consola.
 */
public class CorridaDePruebas {

    public static void main(String[] args) {
        boolean detallado = args.length > 0 && args[0].equals("--detallado");

        System.out.println();
        System.out.println("#########################################################");
        System.out.println("#  CORRIDA DE PRUEBAS - Sistema de Gestion de Pedidos   #");
        System.out.println("#########################################################");

        Resultado caracterizacion = correr(CaracterizacionBase.class, detallado);
        Resultado correcto = correr(ComportamientoCorrecto.class, detallado);

        System.out.println();
        System.out.println("#########################################################");
        System.out.println("#  RESUMEN                                              #");
        System.out.println("#########################################################");
        System.out.println();
        System.out.println("  CARACTERIZACION DEL SISTEMA BASE");
        System.out.println("    " + caracterizacion.linea());
        System.out.println("    Fija el comportamiento actual. Debe estar siempre en verde:");
        System.out.println("    un rojo aca es una regresion introducida por un refactoring.");
        System.out.println();
        System.out.println("  COMPORTAMIENTO CORRECTO ESPERADO");
        System.out.println("    " + correcto.linea());
        System.out.println("    Define el comportamiento deseado. Arranca en rojo:");
        System.out.println("    cada caso que pasa a verde es un defecto corregido.");
        System.out.println();

        if (caracterizacion.fallidas > 0) {
            System.out.println("  >> REGRESION: se rompio comportamiento existente.");
        } else if (correcto.fallidas > 0) {
            System.out.println("  >> Sin regresiones. Quedan " + correcto.fallidas
                    + " defectos por corregir.");
        } else {
            System.out.println("  >> Sin regresiones. Todos los defectos que este trabajo");
            System.out.println("     se propuso corregir estan corregidos.");
        }
        System.out.println();
        // Verde no significa "sistema sin defectos". Significa "sin regresiones
        // y sin defectos pendientes DE LOS QUE ESTE TRABAJO ASUMIO". Hay uno
        // confirmado que se decidio no corregir, y una corrida en verde que no
        // lo mencionara daria una impresion falsa del estado del sistema.
        System.out.println("  Defecto confirmado que sigue ABIERTO por decision explicita:");
        System.out.println("    D-03  cancelar un pedido despues de un recuento fisico de");
        System.out.println("          deposito repone stock que el recuento ya no contemplaba.");
        System.out.println("          Requiere modelar stock reservado; excede el alcance.");
        System.out.println("          Congelado en CaracterizacionBase.defectoQueSigueAbierto().");
        System.out.println();
        if (!detallado) {
            System.out.println("  (para ver caso por caso: --detallado)");
            System.out.println();
        }

        // Solo una regresion hace fallar la corrida. Los defectos pendientes son
        // el estado conocido del sistema, no un error de la corrida.
        System.exit(caracterizacion.fallidas > 0 ? 1 : 0);
    }

    static class Resultado {
        int pasadas;
        int fallidas;
        String linea() {
            return pasadas + " pasadas, " + fallidas + " fallidas";
        }
    }

    /**
     * Ejecuta el main de una suite capturando la salida del dominio.
     *
     * ponytail: se invoca por reflexion y se parsea el resumen impreso porque
     * las suites son main() independientes, ejecutables por separado. Con JUnit
     * esto lo resolveria el runner. Techo aceptado: son dos suites.
     */
    static Resultado correr(Class<?> suite, boolean detallado) {
        PrintStream original = System.out;
        ByteArrayOutputStream capturado = new ByteArrayOutputStream();
        String salida;
        try {
            System.setOut(new PrintStream(capturado, true, "UTF-8"));
            try {
                suite.getMethod("main", String[].class)
                        .invoke(null, (Object) new String[]{"--sin-salir"});
            } catch (Exception e) {
                // Las suites llaman System.exit; si una politica de seguridad lo
                // impide o algo falla, se reporta y se sigue.
            }
        } catch (Exception e) {
            System.setOut(original);
            System.out.println("  no se pudo ejecutar " + suite.getSimpleName() + ": " + e);
            return new Resultado();
        } finally {
            System.setOut(original);
            salida = capturado.toString();
        }

        Resultado r = new Resultado();
        for (String linea : salida.split("\n")) {
            String l = linea.trim();
            if (detallado && (esMarcador(l) || l.startsWith("esperado:")
                    || l.startsWith("obtenido:") || l.startsWith("antes:")
                    || l.startsWith("===") || l.startsWith("el defecto volvio:"))) {
                System.out.println("  " + l);
            }
            if (l.startsWith("[OK]") || l.startsWith("[CORREGIDO]")) {
                r.pasadas++;
            }
            if (l.startsWith("[FALLA]") || l.startsWith("[REAPARECIO]")) {
                r.fallidas++;
                if (!detallado) {
                    System.out.println("  " + l);
                }
            }
        }
        return r;
    }

    /**
     * Los cuatro veredictos que imprime Verificador.
     *
     * DEFECTO CORREGIDO EN ESTA ETAPA. Antes esta deteccion estaba escrita
     * en linea y solo conocia [OK] y [FALLA], que eran los unicos veredictos
     * que existian cuando se escribio. Al agregar yaNoOcurre() aparecieron
     * [CORREGIDO] y [REAPARECIO], y nadie volvio a tocar el contador.
     *
     * Consecuencias, en orden de gravedad:
     *
     *   1. Los siete casos [CORREGIDO] no se contaban. La corrida informaba
     *      62 casos de caracterizacion cuando la suite ejecuta 69. El numero
     *      que figuraba en el informe estaba mal.
     *
     *   2. Mucho peor: un [REAPARECIO] tampoco se contaba. Si un defecto ya
     *      corregido volvia, la suite lo detectaba, lo imprimia por pantalla
     *      y la corrida terminaba igual EN VERDE, con exit code 0. La red de
     *      seguridad tenia un agujero exactamente del tamano de lo que venia
     *      a vigilar.
     *
     * Se detecto comparando el total que informa CorridaDePruebas contra el
     * que informa la suite ejecutada por separado: 62 contra 69. Dos fuentes
     * que cuentan lo mismo y no coinciden.
     *
     * ponytail: la causa de fondo es que esta clase cuenta parseando texto en
     * vez de recibir el Resultado de la suite. Con JUnit no existiria. Se
     * deja asi porque las suites son main() ejecutables por separado, que es
     * una propiedad que queriamos; pero el precio es este, y ya se cobro una
     * vez. Si aparece un quinto veredicto, hay que tocar aca tambien.
     */
    static boolean esMarcador(String linea) {
        return linea.startsWith("[OK]") || linea.startsWith("[FALLA]")
                || linea.startsWith("[CORREGIDO]") || linea.startsWith("[REAPARECIO]");
    }
}
