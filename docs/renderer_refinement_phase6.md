# Renderer refinement - fase 6

Ajustes implementados com foco em estabilidade e legibilidade visual:

- ampliação controlada das dimensões de módulos FV, microinversores, barramentos, disjuntores e barramento terra;
- inclusão de caixas DC inferiores no módulo FV, sem alterar o modelo elétrico existente;
- melhoria da leitura visual dos barramentos BARL / BARN / BARPE, com rótulo multilinha e faixa de cor funcional;
- melhoria da legibilidade do texto com suporte a multilinha no renderer;
- melhoria do dimensionamento e da leitura dos disjuntores, preservando a lógica de polos existente;
- pequenos ajustes de bounding box e posicionamento de portas para acompanhar a nova geometria.

Escopo propositalmente conservador:

- nenhum contrato público de ViewModel foi alterado;
- nenhum enum, DTO ou regra de validação foi modificado;
- nenhuma alteração de persistência foi feita;
- nenhuma alteração estrutural de navegação foi feita.

Arquivo principal revisado:

- `app/src/main/java/br/com/copelsolardiagram/ui/screens/editor/ComponentRenderer.kt`
