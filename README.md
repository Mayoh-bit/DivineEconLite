插件定位

DivineEconLite 是一款轻量经济插件，主打三件事：杀怪给钱，指令收费，死亡惩罚只扣钱包并保留银行余额。它可以独立运行，也可以在安装 Vault 时向其他插件提供 Economy 服务，方便你后续生态联动。

一，功能清单

1）钱包与银行双账户
钱包用于日常消费与指令收费。
银行用于存放资金，死亡惩罚不会影响银行。

2）击杀掉钱
支持按怪物类型设置奖励区间（最小值与最大值），支持默认值兜底。

3）指令收费
可在 config.yml 里配置需要收费的指令与金额。
支持免收费权限。
支持二次确认模式，避免误触扣费。

4）死亡惩罚
按比例扣除钱包余额，默认 50%。
银行余额不受影响。

5）Vault 兼容
检测到 Vault 时自动注册 Economy 服务，其他插件可通过 Vault 读取与修改玩家钱包余额。

二，适用版本与依赖

支持：Spigot 1.20.1 及兼容实现
Java：17
依赖：无硬依赖
可选：Vault，用于对外提供 Economy 服务

三，命令与权限

命令

/money 查看钱包与银行
/pay <玩家> <金额> 转账，扣钱包
/bank deposit <金额> 存入银行
/bank withdraw <金额> 取出到钱包
/divineecon reload 重载配置
/divineecon set <玩家> <wallet|bank> <金额> 管理员设置余额

权限

divineecon.use 允许使用 /money
divineecon.pay 允许使用 /pay
divineecon.bank 允许使用 /bank
divineecon.admin 允许使用 /divineecon set
divineecon.reload 允许使用 /divineecon reload
divineecon.fees.bypass 免指令收费
divineecon.death.bypass 免死亡惩罚

四，配置示例

插件首次启动会生成 config.yml。你可以按下面思路调整数值。

经济参数

starting-balance: 0
death-penalty-percent: 50
currency-name: 金币

击杀奖励

kill-rewards.default.min: 2
kill-rewards.default.max: 4
kill-rewards.by-entity.ZOMBIE.min: 4
kill-rewards.by-entity.ZOMBIE.max: 8

同理可加更多实体类型，实体名使用 Bukkit 的 EntityType 枚举名。

指令收费

command-fees.enabled: true
command-fees.confirm: true
command-fees.list:
spawn: 20
home: 30
sethome: 80
tpa: 30
rtp: 120

你可继续加更多指令，按“主命令”填写即可，例如 warp，back。

五，安装与构建

1）构建

解压源码包后在目录执行：mvn package
生成文件在 target/divineeconlite-1.0.0.jar

2）安装

把 jar 放入 plugins 目录，启动服务器生成配置。
如需对外提供经济接口，安装 Vault 并重启。

六，常见问题

1）与现有经济插件冲突

若你已经使用其它经济插件并且它同样向 Vault 提供 Economy 服务，建议二选一。
DivineEconLite 的定位是轻量一体化经济与收费体系。

2）如何只用指令收费，不用本插件做经济

可以将 kill-rewards 设为 0，death-penalty-percent 设为 0。插件仍可用于指令收费与银行体系。

七，开源与许可

MIT License。允许二次修改与再发布，保留署名信息即可。
