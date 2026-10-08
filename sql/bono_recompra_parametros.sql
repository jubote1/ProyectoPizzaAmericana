INSERT INTO general.parametros (valorparametro, valornumerico, valortexto) VALUES
('BONOSEGUNDOSCORREO', 20, 'Segundos entre un aviso de bono y el siguiente'),
('BONOMAXCORREOSNOCHE', 200, 'Maximo de avisos de bono por noche; los que falten salen la noche siguiente')
ON DUPLICATE KEY UPDATE valortexto = valortexto;
SELECT valorparametro, valornumerico FROM general.parametros WHERE valorparametro LIKE 'BONO%';
