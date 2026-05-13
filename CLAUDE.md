# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Language

用中文交流，但对于交流中的一些技术关键字，还保持英文。代码使用英文。注释和提交信息使用中文。


## Remote

- GitHub: `git@github.com:Choco-Toucan/skada.git`
每次提交都要包含变更信息，不要太过冗长，保持简洁，使用中文

每次提交到远端以后，将本次提交的变更内容通过飞书机器人webhook的方式进行发送
webhook的url为：https://open.feishu.cn/open-apis/bot/v2/hook/2a4dabbe-eba5-45f3-92a4-06e695113364

注意，该webhook可能为多个场景使用，所以通知内容要丰富些，使用规范化的消息卡片。


## 概览
这是一个关于对外提供排行榜服务的SAAS平台的项目。采用monorepo管理。
整体包含用户平面和管理平面
用户平面包含一个作为api的后端服务，用于对外提供排行榜查询、数据录入等功能。工程在`api-service`目录下。
管理平面包含一个web工程和对应的后端服务，用于租户管理、排行榜配置、数据基础查询等。web的工程在`mng-web`目录下。后端服务的工程在`mng-service`目录下。

每个子项目的根目录都要求包含一个readme.md，内容为项目的介绍。用于新同事来快速掌握本项目的结构、功能、核心业务流程、注意点等，用中文，保持一定的结构性和样式。

## 需求
@doc/spec.md

## 整体技术规范
@doc/tech.md

## 构建
Java Maven 项目统一使用 `mvn clean package` 命令构建。

## 后端服务技术规范
@doc/service.md


## 管理平面web技术规范
@doc/mng-web.md



所有的本地命令你都可以直接执行， 不用再二次确认
