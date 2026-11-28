# Arma libs/firma-encuesta.jar: la firma del QR de la encuesta, como libreria
# para la TIENDA.
#
# POR QUE UN JAR Y NO COPIAR LA CLASE. El POS firma y el central revisa. Si la
# clase se escribe dos veces, basta UN caracter de diferencia en el texto que se
# firma para que el central rechace TODAS las encuestas de la tienda. El fuente
# vive solo en el central; la tienda recibe el compilado.
#
# POR QUE UN JAR APARTE Y NO DENTRO DE motor-pedido.jar. Ese jar es el motor de
# pedidos; meterle seguridad de encuestas haria que su nombre mienta y que un
# cambio en la encuesta obligue a mover el motor.
#
# No depende de nada: solo del JDK (javax.crypto viene con Java).
#
# Uso:   .\scripts\armar-firma-jar.ps1
# Deja:  build\firma-encuesta.jar  y la copia en ProyectoTiendaAmericanaCliente\libs\
#
# Despues hay que COMMITEAR el jar en el repositorio de la tienda. Y ojo: el zip
# guarda la fecha de cada entrada, asi que el archivo siempre aparece modificado
# aunque la clase no cambie. Para saber si cambio de verdad, comparar las clases.

$ErrorActionPreference = "Stop"
$raiz = Split-Path -Parent $PSScriptRoot
$pos  = Join-Path (Split-Path -Parent $raiz) "ProyectoTiendaAmericanaCliente"
$jdk  = "C:\Program Files\Java\jdk-16.0.2\bin"

$fuente = Join-Path $raiz "src\main\java\capaSeguridad\FirmaEncuestaQR.java"
$salida = Join-Path $raiz "build\clases-firma"
if (Test-Path $salida) { Remove-Item $salida -Recurse -Force }
New-Item -ItemType Directory -Force $salida | Out-Null

Write-Host "Compilando la firma..." -ForegroundColor Cyan
& "$jdk\javac.exe" -nowarn -encoding UTF-8 -d $salida $fuente
if ($LASTEXITCODE -ne 0) { throw "La compilacion fallo." }

$jar = Join-Path $raiz "build\firma-encuesta.jar"
& "$jdk\jar.exe" --create --file $jar -C $salida capaSeguridad
if ($LASTEXITCODE -ne 0) { throw "No se pudo armar el jar." }

$n = (& "$jdk\jar.exe" --list --file $jar | Where-Object { $_ -like "*.class" }).Count
Write-Host "Listo: $jar  ($n clases)" -ForegroundColor Green

$destino = Join-Path $pos "libs\firma-encuesta.jar"
if (Test-Path (Split-Path -Parent $destino)) {
  Copy-Item $jar $destino -Force
  Write-Host "Copiado a $destino" -ForegroundColor Green
  Write-Host "Falta commitearlo en el repositorio de la tienda." -ForegroundColor Yellow
} else {
  Write-Host "No encontre $pos : copie el jar a mano." -ForegroundColor Yellow
}
