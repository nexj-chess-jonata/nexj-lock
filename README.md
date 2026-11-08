# NexJ Lock

Bloqueador de apps para Android: escolha quais apps proteger e eles pedem PIN ou biometria ao abrir.
Sem internet (o app nem tem permissão de INTERNET), sem anúncios, sem conta.

## Como funciona
1. Um serviço em segundo plano usa o **Acesso de uso** para saber qual app foi aberto.
2. Se o app está na sua lista, a tela de PIN aparece por cima (permissão **Exibir sobre outros apps**).
3. Acertou o PIN (ou a biometria)? O app abre e fica liberado pelo tempo que você escolher depois de sair dele.

## Recursos
- PIN de 4 a 8 dígitos (guardado só como hash PBKDF2) e biometria opcional
- 5 erros = espera de 30 segundos
- O próprio painel do NexJ Lock também pede PIN
- Tela de bloqueio e painel não aparecem em prints nem nos apps recentes
- Religa a proteção sozinho depois de reiniciar o celular
- Tempo de re-bloqueio: imediato, 5 s, 30 s ou 2 min
- Ao desligar a tela, tudo volta a ficar bloqueado

## Gerar o APK pelo GitHub
1. Crie um repositório e envie estes arquivos (mantenha a pasta `.github`).
2. Aba **Actions** → **Build APK** → **Run workflow** (leva uns 5 a 10 minutos).
3. Baixe o `nexj-lock-apk` na execução e instale o `.apk` no celular.

## Primeiro uso
1. Abra o app e crie seu PIN.
2. Conceda **Acesso de uso** e **Exibir sobre outros apps** (e, de preferência, notificações e "sem limite de bateria").
3. Marque os apps que quer proteger e toque em **Ligar proteção**.

## Limites honestos
- Em alguns celulares (Xiaomi, Samsung, Huawei…) é preciso também liberar o app em "início automático" / "sem restrição de bateria", senão o sistema desliga o serviço.
- Pode aparecer o app por uma fração de segundo antes da tela de PIN cobrir (limitação do Android sem acessibilidade).
- Quem tem acesso às Configurações pode desinstalar o NexJ Lock. Para dificultar, proteja também "Configurações" na lista.
