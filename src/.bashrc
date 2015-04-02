# .bashrc

# variable definition
Color_Off="\[\e[0m\]"       # Text Reset
Black='\[\e[1;30m\]'        # Black
Red="\[\e[1;31m\]"          # Red
Green="\[\e[1;32m\]"        # Green
Yellow="\[\e[1;33m\]"       # Yellow
Blue='\[\e[1;34m\]'         # Blue
Purple='\[\e[1;35m\]'       # Purple
Cyan='\[\e[1;36m\]'         # Cyan
White='\[\e[1;37m\]'        # White

# PS1 definition 
source ~/git-prompt.sh
# PS1="\[\e[1;31m\]\u@\h\[\e[0m\] \W $(__git_ps1)\$ "
PS1="$Green\u@\h$Color_Off \W $Yellow"'$(__git_ps1)'"$Color_Off\$ "
export TERM=xterm

# Source global definitions
if [ -f /etc/bashrc ]; then
	. /etc/bashrc
fi

# Aliases and color definition
alias vi='vim'
alias logp='git log --pretty=oneline'
alias evalabas='cd && eval $(sh denv.sh)'
alias ll='ls -ltr --color'
LS_COLORS='di=1:di=42:fi=0:ln=32:pi=5:so=5:bd=5:cd=5:or=32:mi=0:ex=32:*.rpm=90'
export LS_COLORS
export LANG=en_US.utf8
