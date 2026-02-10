# First init python
sudo apt update
sudo apt install -y python3-full python3-venv

python3 -m venv .venv
source .venv/bin/activate

python -m pip install --upgrade pip
pip install -r requirements.txt

# In chaos-arena-scripter/
PS E:\repository\chaos-repository\services\chaos-arena-scripter> python -m pip install -U pip
PS E:\repository\chaos-repository\services\chaos-arena-scripter> python -m pip install -e .

# SCALE
python -m chaos_scripter.cli.cli_submit --type SCALE --target deployment/cart --value 6

# KILL_PODS
python -m chaos_scripter.cli.cli_submit --type KILL_PODS --target "app=cart" --value 30

# ROLLBACK SCALE về 3 replicas
python -m chaos_scripter.cli.cli_submit --type ROLLBACK --target "SCALE|default|cart|3"

# Clean Code (Optional)
pip install black isort ruff
ruff check . --fix
isort .
black .
