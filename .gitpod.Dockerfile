FROM gitpod/workspace-full

USER gitpod


RUN sudo apt-get update && \
    sudo apt-get install -y zsh

ENV ZSH_THEME cloud

RUN wget https://github.com/robbyrussell/oh-my-zsh/raw/master/tools/install.sh -O - | zsh
