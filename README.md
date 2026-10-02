# Cajero automático — pruebas con JUnit 5 y Mockito

**Autor:** Hilario Sebastian Espinoza Garcia

## Cómo correr las pruebas

    ./probar.sh final

## Qué prueba cada clase

| Clase de prueba | Qué prueba | Qué dobles usa | Cuántas pruebas |
| --- | --- | --- | --- |
| CajeroConDoblesAManoTest | El flujo básico de retiros exitosos y fallidos. | fakes hechos a mano y una lambda | 2 |
| StubsTest | El comportamiento del cajero ante diferentes respuestas simuladas de sus dependencias. | stubs de Mockito| 7 |
| InteraccionesTest | Que los métodos de las dependencias sean llamados (o no) en el orden, cantidad y con los argumentos correctos. | mocks programáticos| 7 |
| AnotacionesTest | Las mismas validaciones que InteraccionesTest, pero utilizando la sintaxis declarativa de JUnit y Mockito. | anotaciones (`@Mock`, `@Captor`, `@ExtendWith`) | 3 |
| QuejasCajeroTest | Casos límite y bugs reportados en producción. | mocks de Mockito puro (sin la extensión estricta) | 5 |
| RelojTest | La lógica dependiente del tiempo (como el límite diario que se reinicia a medianoche) considerando zonas horarias. | `Clock.fixed` (objeto real, no mock) y stubs | 3 |

## Boleto de salida

1. ¿Qué diferencia hay entre un stub y un mock? Da un ejemplo de cada uno de hoy.

Stub se usa para preparar el escenario ("estado"). Se le dice a la dependencia falsa cómo debe responder cuando la llamen, pero no nos importa si la llaman o no.
Ejemplo de hoy: when(repositorio.buscar("001")).thenReturn(Optional.of(cuenta));
Mock se usa para verificar el comportamiento ("interacciones"). Nos sirve para comprobar al final de la prueba que un método se llamó exactamente las veces esperadas y con los argumentos correctos.
Ejemplo de hoy: verify(repositorio, times(1)).guardar("001", cuenta);

2. ¿Por qué la prueba inútil de la MP-6 salía en verde con el cajero roto?

Porque la prueba evaluaba un caso donde el límite ya se excedía por un margen amplio (ej. retirar 500 cuando ya se habían retirado 7600, sumando 8100). Como 8100 excede los 8000 tanto en el código roto (con >=) como en el código arreglado (con >), la prueba pasaba siempre y daba un "falso positivo". Para atrapar el error del >= era obligatorio probar exactamente la frontera (retirar 500 cuando ya se sacaron 7500, dando los 8000 exactos).

3. ¿Qué NO mockearías en un proyecto de Spring Boot como el de la Semana 3, y por qué?

Probablemente aquellos que son contenedores de datos que solo pasan datos de una capa a otra como la clase Empleado