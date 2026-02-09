sudo apt update
sudo apt install -y python3-full python3-venv

python3 -m venv .venv
source .venv/bin/activate

python -m pip install --upgrade pip
pip install -r requirements.txt

PS E:\repository\chaos-repository\services\chaos-arena-scripter> python -m pip install -U pip
PS E:\repository\chaos-repository\services\chaos-arena-scripter> python -m pip install -e .