# NightLock 🔒

**Bloqueo automático de WiFi: 11:00 PM → 5:00 AM · Todos los días**

---

## ¿Qué hace esta app?

- A las **11:00 PM** activa automáticamente una VPN local que descarta todo el tráfico WiFi
- A las **5:00 AM** desactiva la VPN y el WiFi vuelve a funcionar
- Durante el horario de bloqueo, **el botón de desactivar desaparece** — no puedes quitarlo desde la app
- Tiene protección **Device Admin** para evitar la desinstalación en el horario de bloqueo
- Los **datos móviles siguen funcionando** — solo bloquea WiFi

---

## Cómo abrir en Android Studio

1. Abre Android Studio
2. **File → Open** → selecciona la carpeta `NightLock`
3. Espera que Gradle sincronice
4. Conecta tu Moto G34 5G con **Depuración USB** activada
5. Presiona **Run ▶**

---

## Primera vez que abres la app

La app te pedirá dos permisos al abrirla por primera vez:

1. **Permiso VPN** — necesario para el bloqueo de tráfico
   - Aparece un diálogo del sistema: pulsa **OK**

2. **Permiso de Administrador de Dispositivo** — protege contra desinstalación
   - Pulsa **Activar** en la pantalla que aparece

Después de aceptar ambos, la app queda **activa permanentemente**.
Los horarios se reprograman solos en cada reinicio del teléfono.

---

## Lógica del bloqueo

```
Horario actual       Estado
─────────────────────────────────────────
00:00 – 04:59        🔒 BLOQUEADO (VPN activa)
05:00 – 22:29        ✅ Libre
22:30 – 23:59        🔒 BLOQUEADO (VPN activa)
```

---

## Estructura del proyecto

```
NightLock/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/nightlock/app/
│   │   ├── ui/
│   │   │   └── MainActivity.java          ← UI principal
│   │   ├── service/
│   │   │   └── NightLockVpnService.java   ← VPN que bloquea tráfico
│   │   ├── receivers/
│   │   │   ├── BootReceiver.java          ← Reprograma al reiniciar
│   │   │   ├── ScheduleReceiver.java      ← Dispara inicio/fin de bloqueo
│   │   │   └── NightLockAdminReceiver.java ← Anti-desinstalación
│   │   └── util/
│   │       └── ScheduleManager.java       ← Lógica de AlarmManager
│   └── res/
│       ├── layout/activity_main.xml
│       ├── values/{colors,strings,themes}.xml
│       ├── drawable/{ic_lock,bg_card,bg_lock_panel}.xml
│       └── xml/device_admin.xml
```

---

## Cómo funciona el bloqueo (técnico)

Android 10+ no permite que apps de terceros apaguen el WiFi directamente.
NightLock usa una **VPN local** que:
1. Intercepta todo el tráfico de red del dispositivo
2. En lugar de reenviarlo, lo **descarta** (drainPackets)
3. El WiFi sigue "conectado" pero sin acceso a internet

Los datos móviles no pasan por la VPN local → **siguen funcionando**.

---

## ¿Cómo evitar que me saltee el bloqueo?

La app tiene tres capas:
1. **UI bloqueada** — el botón desaparece en horario de bloqueo
2. **VPN siempre encendida** — aunque cierres la app, sigue activa
3. **Device Admin** — no puedes desinstalar la app sin revocar el permiso
   (que solo puedes revocar fuera del horario de bloqueo)

El único escape es un **reseteo de fábrica** — que nadie va a hacer por WiFi.
