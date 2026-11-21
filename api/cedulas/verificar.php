<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");

if ($_SERVER["REQUEST_METHOD"] !== "POST") {
    http_response_code(405);
    echo json_encode(["mensaje" => "Este endpoint solo acepta POST"]);
    return;
}

$data = json_decode(file_get_contents("php://input"));
$cedula = isset($data->cedula) ? trim((string) $data->cedula) : "";

if (!preg_match("/^[0-9]{10}$/", $cedula)) {
    http_response_code(400);
    echo json_encode(["mensaje" => "La cédula debe tener exactamente 10 dígitos"]);
    return;
}

$ruta = __DIR__ . "/../data/cedulas.json";
$registradas = json_decode(file_get_contents($ruta), true);
if (!is_array($registradas)) {
    $registradas = [];
}

foreach ($registradas as $persona) {
    if (($persona["cedula"] ?? "") === $cedula) {
        echo json_encode([
            "estado" => true,
            "cedula" => $cedula,
            "nombre" => $persona["nombre"]
        ], JSON_UNESCAPED_UNICODE);
        return;
    }
}

echo json_encode([
    "estado" => false,
    "cedula" => $cedula
], JSON_UNESCAPED_UNICODE);
