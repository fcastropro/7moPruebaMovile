# Guía para el video del deber — Agenda UNIANDES

Estudiante: Fernando Castro.  
Proyecto Android: `TAREA_3_FernandoCastro`.  
Web Service: `C:\Users\Fernando\code\agenda_uniandes\backend\public` en Homestead, `http://192.168.56.56:8088`.  
Base: `db_agenda24`.

La recuperación de contraseña compara la cédula y el correo dentro de la aplicación, sin enviar un email. Sirve para este deber académico. En un sistema real haría falta otro medio para comprobar la identidad, por ejemplo un código de un solo uso. Esta pantalla no debe presentarse como recuperación segura de producción.

## Orden de la demostración

1. Menú de inicio.
2. Crear cuenta y mostrar el rechazo si la cédula ya existe.
3. Iniciar sesión con clave incorrecta y luego con la correcta.
4. Listado vacío o solo con los contactos de esa cuenta.
5. Crear un contacto.
6. Editarlo y ver el cambio en la lista.
7. Eliminar: Cancelar deja el contacto; Confirmar lo quita y muestra el aviso.
8. Cerrar sesión y comprobar que Iniciar sesión vuelve a pedir la clave.
9. Recuperar contraseña e ingresar con la clave nueva.
10. Mostrar, en Android Studio y en el PHP, que otro usuario no puede editar ni borrar contactos ajenos.

Cuentas de prueba ya creadas, con datos ficticios:

| Cédula | Nombre | Correo | Contraseña actual |
| --- | --- | --- | --- |
| 1717171717 | Ana Demo | ana.demo@uniandes.test | ClaveNueva2 |
| 1818181818 | Luis Demo | luis.demo@uniandes.test | ClaveSegura1 |

## 1. Crear cuenta

- Kotlin: `RegistroActivity.kt`, función `registrar`.
- XML: `activity_registro.xml`.
- PHP: `registrar.php`.
- SQL: `INSERT INTO persona (ci_persona, nom_persona, ape_persona, clave_persona, correo_persona) VALUES (?, ?, ?, ?, ?)`.
- La contraseña se guarda con `password_hash`. La cédula duplicada responde «Ya existe una cuenta con esa cédula» por la clave única `ci_persona_UNIQUE`.
- Validaciones: cédula de 10 dígitos, correo válido, campos obligatorios, contraseña de 8 caracteres y confirmación igual.
- Al terminar abre `LoginActivity`.

## 2. Iniciar sesión

- Kotlin: `LoginActivity.kt`, función `ingresar`. El token queda en preferencias privadas de la app, en `Sesion.kt`.
- XML: `activity_login.xml`.
- PHP: `login.php`.
- SQL: `SELECT cod_persona, nom_persona, ape_persona, clave_persona FROM persona WHERE ci_persona = ?`.
- La comparación usa `password_verify`. Si una cuenta antigua todavía tiene la clave en texto plano, entra solo si coincide y en ese momento se reemplaza por un hash. No se devuelve ni se escribe la contraseña.
- El servidor crea una fila en `sesion` con el hash del token, no el token en claro.
- Si falla: «Cédula o contraseña incorrecta».

## 3. Recuperar contraseña

- Kotlin: `RecuperarClaveActivity.kt`, función `recuperar`.
- XML: `activity_recuperar_clave.xml`.
- PHP: `recuperar.php`.
- SQL: `UPDATE persona SET clave_persona = ? WHERE cod_persona = ?` y `DELETE FROM sesion WHERE persona_cod_persona = ?`.
- Solo cambia la clave si la cédula y el correo coinciden. La respuesta de error no dice cuál de los dos falló.

## 4. Listar contactos

- Kotlin: `ContactosActivity.kt`, función `cargarContactos`; modelo `Contacto.kt`; adaptador `ContactoAdapter.kt`.
- XML: `activity_contactos.xml` y `item_contacto.xml`.
- PHP: `listar.php`.
- SQL: `SELECT ... FROM contacto WHERE persona_cod_persona = ? ORDER BY cod_contacto DESC`.
- El `cod_persona` sale de la sesión. El cliente no puede pedir los contactos de otra persona.
- Cada tarjeta muestra foto o `ic_avatar`, nombre, apellido, teléfono, Editar y Eliminar.
- Sin registros: «No hay contactos registrados».

## 5. Crear contacto

- Kotlin: `CrearContactoActivity.kt`, funciones `guardar` y `enviarJson`.
- XML: `activity_crear_contacto.xml`.
- PHP: `crear.php`.
- SQL: `INSERT INTO contacto (..., persona_cod_persona) VALUES (..., ?)` con el dueño de la sesión.
- La foto, si se elige, se guarda como archivo y en MySQL solo queda la ruta `fotos/...`.

## 6. Editar contacto

- Kotlin: `EditarContactoActivity.kt`, función `guardar`. Se abre desde `ContactosActivity.abrirEdicion`.
- XML: `activity_editar_contacto.xml`.
- PHP: `editar.php`.
- SQL: `UPDATE contacto SET nom_contacto = ?, ape_contacto = ?, telefono_contacto = ?, email_contacto = ? WHERE cod_contacto = ? AND persona_cod_persona = ?`.
- Es un UPDATE del mismo registro. Si el contacto es de otro usuario, la respuesta es «El contacto no existe».

## 7. Eliminar contacto

- Kotlin: `ContactosActivity.confirmarEliminacion` y `eliminar`.
- El diálogo ofrece Cancelar y Confirmar. Cancelar no llama al servidor.
- PHP: `eliminar.php`.
- SQL: `DELETE FROM contacto WHERE cod_contacto = ? AND persona_cod_persona = ?`.
- La lista se actualiza solo si `estado` es verdadero y se muestra «Contacto eliminado correctamente».

## 8. Cerrar sesión

- Kotlin: `ContactosActivity.cerrarSesion`.
- PHP: `cerrar.php`.
- SQL: `DELETE FROM sesion WHERE token_hash = ? AND persona_cod_persona = ?`.
- También se borra el token local. Sin sesión, Contactos, Crear y Editar regresan al inicio de sesión.

## Red

`Red.kt` envía Volley a `http://192.168.56.56:8088/` con el encabezado `Authorization: Bearer` cuando hay sesión. Las respuestas usan `estado`, `mensaje` y, cuando aplica, `datos`.

## Migración

`C:\Users\Fernando\code\agenda_uniandes\migracion_cuentas.sql` amplía `clave_persona` a `varchar(255)` y crea la tabla `sesion`. No borra personas ni contactos.
