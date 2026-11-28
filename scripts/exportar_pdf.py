"""Exporta o README para PDF A4, com visual academico preto e branco."""
from pathlib import Path
import mistune
from weasyprint import HTML, CSS

RAIZ = Path(__file__).resolve().parent.parent
conteudo = (RAIZ / "README.md").read_text(encoding="utf-8")
pagina = mistune.create_markdown(plugins=["table"])(conteudo)
estilos = """
@page {
  size: A4;
  margin: 19mm 18mm 19mm 19mm;
  @bottom-left { content: "PROJETO DE SISTEMAS OPERACIONAIS"; font: 8pt sans-serif; color: #555; }
  @bottom-right { content: counter(page); font: 9pt sans-serif; }
}
body { font-family: "Liberation Sans", Arial, sans-serif; font-size: 10pt;
       line-height: 1.48; color: #171717; }
h1 { font-size: 22pt; line-height: 1.14; margin: 4mm 0 5mm;
     border-bottom: 2px solid #333; padding-bottom: 4mm; }
h2 { font-size: 13.5pt; margin: 6mm 0 3mm; break-after: avoid; }
h3 { font-size: 11.5pt; margin: 5mm 0 2mm; break-after: avoid; }
p { margin: 0 0 3mm; orphans: 3; widows: 3; text-align: justify; }
li { margin: 1mm 0; }
ul, ol { padding-left: 6mm; }
a { color: black; text-decoration: underline; overflow-wrap: anywhere; }
table { border-collapse: collapse; width: 100%; font-size: 8.7pt; margin: 3mm 0; }
th, td { padding: 1.8mm 2mm; text-align: left; vertical-align: top;
         border-bottom: .6pt solid #bbb; }
th { background: #eee; border-top: 1px solid #333; border-bottom: 1px solid #333; }
tr { break-inside: avoid; }
code { font-family: "DejaVu Sans Mono", monospace; font-size: 8pt;
       background: #f0f0f0; overflow-wrap: anywhere; }
pre { white-space: pre-wrap; overflow-wrap: anywhere; font-size: 8pt;
      padding: 3mm; border: 1px solid #bbb; background: #f4f4f4; }
pre code { background: transparent; }
blockquote { padding-left: 4mm; border-left: 2px solid #555; color: #333; }
"""
destino = RAIZ / "Relatorio_Simulador_Sistema_Arquivos.pdf"
HTML(
    string=f'<html lang="pt-BR"><meta charset="utf-8"><body>{pagina}</body></html>',
    base_url=str(RAIZ)
).write_pdf(str(destino), stylesheets=[CSS(string=estilos)])
print("Relatorio criado:", destino)
