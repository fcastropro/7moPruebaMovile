<?php
$ruta = parse_url($_SERVER["REQUEST_URI"], PHP_URL_PATH);

if ($ruta === "/cedulas/verificar") {
    require __DIR__ . "/cedulas/verificar.php";
    return true;
}

http_response_code(404);
header("Content-Type: application/json; charset=UTF-8");
echo json_encode(["mensaje" => "Ruta no encontrada"]);
return true;
