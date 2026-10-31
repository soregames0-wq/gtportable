# GT Portable (Gran Turismo PSP, recomp para Android)

Este projeto é a **casca Android** do recomp: seletor de ROM, controles de corrida
personalizáveis na tela e a ponte JNI. O **núcleo recompilado do jogo** não vem pronto:
ele é gerado a partir da SUA cópia do jogo, no seu PC.

## O que já está pronto
- Seleciona a ISO/CSO (Storage Access Framework), valida o cabeçalho e lembra da última.
- Passa o arquivo ao núcleo por file descriptor (sem copiar 1 GB).
- Controles na tela: volante (slider analógico), acelerador, freio, câmera, L/R (marchas),
  D-pad, Start/Select. Botão "✎ Editar controles": arraste, ajuste o tamanho, salve.
- Sem o núcleo, roda em modo de teste (tela colorida que muda ao tocar nos botões).

## Como gerar o núcleo (no PC)
1. Extraia `PSP_GAME/SYSDIR/EBOOT.BIN` da sua ISO. O EBOOT retail é criptografado:
   é preciso ele descriptografado (o PPSSPP tem opção de salvar o EBOOT descriptografado).
2. Rode a toolchain de recomp de PSP (psprecomp) com o comando de geração (`emit`,
   confira o nome na versão que usar) sobre esse EBOOT. Ela gera fontes em C.
3. Crie `app/src/main/cpp/core/` com um `CMakeLists.txt` que define o alvo `gtcore`,
   contendo as fontes geradas + o runtime (kernel HLE, GPU PSP -> Vulkan/GLES, áudio)
   e exporte as 3 funções descritas no topo de `bridge.cpp`.
   O runtime precisa ler arquivos do jogo pelo `iso_fd` (ISO9660/CSO).
4. Abra a pasta no Android Studio (ele cria o gradle wrapper), Build > Build APK.

## Mapeamento
Os botões usam os bits padrão do PSP. Confira em Opções > Controles do jogo qual botão
é acelerar/frear e reposicione no editor.

## Aviso
O APK gerado contém código derivado do jogo: use só para você, não distribua.
