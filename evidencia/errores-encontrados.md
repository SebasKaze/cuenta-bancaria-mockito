# Errores encontrados en el cajero del compañero

Mi suite (MP-1 a MP-4) atrapó sola **3** de los 5 errores: (las pruebas que fallaron en mp5-companero.txt)

| Queja | Prueba que la reproduce | Qué estaba mal en el código (la línea y por qué) | ¿Mi suite ya lo atrapaba? |
|---|---|---|---|
| 1 | sinSmsSiNoHaySaldo | El método avisar(...) (SMS) estaba antes de cuenta.retirar(...). Si el retiro fallaba por falta de saldo y lanzaba excepción, el mensaje de texto ya se había enviado. | sí|
| 2 | sospechosaNoMueveDinero | La validación validarAntifraude(...) estaba después de cuenta.retirar(...). El dinero ya se había descontado de la cuenta antes de que el antifraude cancelara la operación. | Si |
| 3 | seGuardaUnaVez | Había dos líneas repetidas consecutivas llamando a repositorio.guardar(numeroCuenta, cuenta); | Si |
| 4 | antifraudeCaidoRechaza | En el catch de validarAntifraude, si lanzaba ServicioNoDisponibleException, el código asignaba sospechosa = false en lugar de lanzar OperacionRechazadaException para detener el retiro | No |
| 5 | limiteDiarioExacto | En validarLimiteDiario, el condicional usaba >= 0 (mayor o igual). Esto provocaba que si la suma daba exactamente el límite de $8,000.00, se excediera y rechazara erróneamente | No |