# Arma libs/motor-pedido.jar: el motor de pedidos como libreria para la TIENDA.
#
# POR QUE NO "gradle motorJar". La tarea existe en build.gradle y es la forma
# correcta cuando el central compila, pero este repositorio NO compila con Gradle
# en el equipo de desarrollo: libs/*.jar esta en .gitignore y falta
# PizzaAmericanaCliente.jar, asi que compileJava revienta con cientos de errores
# que no tienen nada que ver con el motor. Como el motor no depende de ninguna de
# esas librerias -solo del JDK y de Gson-, se compila aparte y listo.
#
# Uso:   .\scripts\armar-motor-jar.ps1
# Deja:  build\motor-pedido.jar  y la copia en ProyectoTiendaAmericanaCliente\libs\
#
# Despues hay que COMMITEAR el jar en el repositorio de la tienda: ese si versiona
# sus libs. En el central no se guarda.

# OJO CON "EL JAR CAMBIO". Rearmarlo SIEMPRE va a aparecer como modificado en
# git aunque no se haya tocado una sola clase: el zip guarda la fecha de cada
# entrada y esa fecha cambia en cada corrida. Para saber si cambio de verdad hay
# que comparar las clases, no el archivo:
#   jar --extract --file <el viejo>   y   jar --extract --file <el nuevo>   y diff
# Si las clases dan iguales, no vale la pena subir el jar otra vez.

$ErrorActionPreference = "Stop"
$raiz = Split-Path -Parent $PSScriptRoot
$pos  = Join-Path (Split-Path -Parent $raiz) "ProyectoTiendaAmericanaCliente"
$jdk  = "C:\Program Files\Java\jdk-16.0.2\bin"

# Las clases que van al jar. La lista es a mano a proposito: el paquete del motor
# tiene 16 clases y las otras once importan capaDAOCC, log4j, json-simple o el
# modelo viejo del central, y no caben en la tienda. Si se agrega una clase al
# motor hay que decidir si va aqui, no que se cuele sola.
$fuentes = @(
  "src\main\java\capaModeloCC\motor\MotorSolicitud.java",
  "src\main\java\capaModeloCC\motor\MotorResultado.java",
  "src\main\java\capaControladorCC\motor\CatalogoPos.java",
  "src\main\java\capaControladorCC\motor\MotorPedido.java",
  "src\main\java\capaControladorCC\motor\MotorPedidoV1.java",
  "src\main\java\capaControladorCC\motor\MotorPedidoNoImplementado.java",
  "src\main\java\capaControladorCC\motor\SombraPedido.java"
) | ForEach-Object { Join-Path $raiz $_ }

$gson = Get-ChildItem "$env:USERPROFILE\.gradle\caches\modules-2\files-2.1" -Recurse -Filter "gson-*.jar" |
        Where-Object { $_.Name -notmatch "sources|javadoc" } | Select-Object -First 1
if (-not $gson) { throw "No encontre gson en el cache de Gradle." }

$salida = Join-Path $raiz "build\clases-motor"
if (Test-Path $salida) { Remove-Item $salida -Recurse -Force }
New-Item -ItemType Directory -Force $salida | Out-Null

Write-Host "Compilando el motor..." -ForegroundColor Cyan
& "$jdk\javac.exe" -nowarn -encoding UTF-8 -cp $gson.FullName -d $salida $fuentes
if ($LASTEXITCODE -ne 0) { throw "La compilacion fallo." }

$jar = Join-Path $raiz "build\motor-pedido.jar"
& "$jdk\jar.exe" --create --file $jar -C $salida capaControladorCC -C $salida capaModeloCC
if ($LASTEXITCODE -ne 0) { throw "No se pudo armar el jar." }

$n = (& "$jdk\jar.exe" --list --file $jar | Where-Object { $_ -like "*.class" }).Count
$kb = [math]::Round((Get-Item $jar).Length / 1KB, 1)
Write-Host "Listo: $jar  ($n clases, $kb KB)" -ForegroundColor Green

$destino = Join-Path $pos "libs\motor-pedido.jar"
if (Test-Path (Split-Path -Parent $destino)) {
  Copy-Item $jar $destino -Force
  Write-Host "Copiado a $destino" -ForegroundColor Green
  Write-Host "Falta commitearlo en el repositorio de la tienda." -ForegroundColor Yellow
} else {
  Write-Host "No encontre $pos : copie el jar a mano." -ForegroundColor Yellow
}
