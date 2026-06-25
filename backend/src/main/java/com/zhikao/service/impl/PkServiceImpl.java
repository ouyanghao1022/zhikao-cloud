package com.zhikao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhikao.entity.*;
import com.zhikao.mapper.*;
import com.zhikao.service.PkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import cn.hutool.core.util.StrUtil;

/**
 * PK与排行榜Service实现类
 */
@Service
public class PkServiceImpl implements PkService {

    @Autowired
    private PkTeamMapper teamMapper;
    @Autowired
    private PkTeamMemberMapper teamMemberMapper;
    @Autowired
    private PkMatchMapper matchMapper;
    @Autowired
    private PkMatchRecordMapper matchRecordMapper;
    @Autowired
    private LeaderboardMapper leaderboardMapper;
    @Autowired
    private com.zhikao.mapper.ExamPaperMapper examPaperMapper;
    @Autowired
    private com.zhikao.mapper.ExamPaperQuestionMapper examPaperQuestionMapper;
    @Autowired
    private com.zhikao.mapper.UserMapper userMapper;
    @Autowired
    private com.zhikao.service.SysMessageService sysMessageService;

    @Override
    @Transactional
    public PkTeam createTeam(Long userId, PkTeam team) {
        // 检查用户是否已在其他队伍中
        LambdaQueryWrapper<PkTeamMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(PkTeamMember::getUserId, userId)
                .eq(PkTeamMember::getStatus, 1);
        if (teamMemberMapper.selectCount(memberWrapper) > 0) {
            throw new RuntimeException("您已在其他队伍中，请先退出当前队伍");
        }

        team.setCaptainId(userId);
        team.setCurrentMembers(1);
        team.setTotalScore(0);
        team.setWinCount(0);
        team.setLoseCount(0);
        team.setStatus(1);
        team.setCreatedAt(new Date());
        team.setUpdatedAt(new Date());
        if (team.getMaxMembers() == null) team.setMaxMembers(5);
        if (team.getJoinType() == null) team.setJoinType(1);
        teamMapper.insert(team);

        // 自动将队长加入队伍
        PkTeamMember member = new PkTeamMember();
        member.setTeamId(team.getId());
        member.setUserId(userId);
        member.setRole(1); // 队长
        member.setJoinTime(new Date());
        member.setStatus(1);
        teamMemberMapper.insert(member);

        return team;
    }

    @Override
    @Transactional
    public void joinTeam(Long userId, Long teamId) {
        // 检查是否已在队伍中
        LambdaQueryWrapper<PkTeamMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(PkTeamMember::getUserId, userId)
                .eq(PkTeamMember::getStatus, 1);
        if (teamMemberMapper.selectCount(memberWrapper) > 0) {
            throw new RuntimeException("您已在其他队伍中，请先退出当前队伍");
        }

        // 检查队伍是否存在
        PkTeam team = teamMapper.selectById(teamId);
        if (team == null || team.getStatus() == 0) {
            throw new RuntimeException("队伍不存在或已解散");
        }
        if (team.getCurrentMembers() >= team.getMaxMembers()) {
            throw new RuntimeException("队伍已满员");
        }

        // 加入队伍
        PkTeamMember member = new PkTeamMember();

        // 查询是否曾有记录（用户可能之前退出/被踢出，留有 status=0 的旧记录）
        LambdaQueryWrapper<PkTeamMember> existingWrapper = new LambdaQueryWrapper<>();
        existingWrapper.eq(PkTeamMember::getTeamId, teamId)
                .eq(PkTeamMember::getUserId, userId)
                .last("LIMIT 1");
        PkTeamMember existing = teamMemberMapper.selectOne(existingWrapper);
        if (existing != null) {
            // 已有旧记录 → 重新激活
            existing.setRole(0);
            existing.setJoinTime(new Date());
            existing.setStatus(1);
            teamMemberMapper.updateById(existing);
        } else {
            member.setTeamId(teamId);
            member.setUserId(userId);
            member.setRole(0);
            member.setJoinTime(new Date());
            member.setStatus(1);
            teamMemberMapper.insert(member);
        }

        // 更新队伍人数
        team.setCurrentMembers(team.getCurrentMembers() + 1);
        teamMapper.updateById(team);
    }

    @Override
    @Transactional
    public void leaveTeam(Long userId, Long teamId) {
        // 查找成员记录
        LambdaQueryWrapper<PkTeamMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PkTeamMember::getUserId, userId)
                .eq(PkTeamMember::getTeamId, teamId)
                .eq(PkTeamMember::getStatus, 1);
        PkTeamMember member = teamMemberMapper.selectOne(wrapper);
        if (member == null) {
            throw new RuntimeException("您不在该队伍中");
        }

        // 队长退出则解散队伍
        if (member.getRole() == 1) {
            PkTeam team = teamMapper.selectById(teamId);
            team.setStatus(0);
            teamMapper.updateById(team);
            // 将所有成员标记为退出
            LambdaQueryWrapper<PkTeamMember> allMembers = new LambdaQueryWrapper<>();
            allMembers.eq(PkTeamMember::getTeamId, teamId).eq(PkTeamMember::getStatus, 1);
            PkTeamMember updateMember = new PkTeamMember();
            updateMember.setStatus(0);
            teamMemberMapper.update(updateMember, allMembers);
        } else {
            member.setStatus(0);
            teamMemberMapper.updateById(member);
            // 更新队伍人数
            PkTeam team = teamMapper.selectById(teamId);
            team.setCurrentMembers(Math.max(team.getCurrentMembers() - 1, 1));
            teamMapper.updateById(team);
        }
    }

    @Override
    public List<PkTeam> listTeams() {
        LambdaQueryWrapper<PkTeam> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PkTeam::getStatus, 1)
                .orderByDesc(PkTeam::getTotalScore);
        return teamMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public PkMatch startMatch(Long userId, PkMatch match) {
        // 如果没有指定对手，则随机匹配
        if (match.getTeamBId() == null) {
            // 查找用户所在队伍
            LambdaQueryWrapper<PkTeamMember> memberWrapper = new LambdaQueryWrapper<>();
            memberWrapper.eq(PkTeamMember::getUserId, userId)
                    .eq(PkTeamMember::getStatus, 1);
            PkTeamMember member = teamMemberMapper.selectOne(memberWrapper);
            if (member == null) {
                throw new RuntimeException("请先加入一个队伍");
            }
            match.setTeamAId(member.getTeamId());

            // 随机匹配另一支队伍
            LambdaQueryWrapper<PkTeam> teamWrapper = new LambdaQueryWrapper<>();
            teamWrapper.eq(PkTeam::getStatus, 1)
                    .ne(PkTeam::getId, member.getTeamId());
            List<PkTeam> teams = teamMapper.selectList(teamWrapper);
            if (teams.isEmpty()) {
                throw new RuntimeException("暂无其他可匹配的队伍");
            }
            PkTeam randomTeam = teams.get((int) (Math.random() * teams.size()));
            match.setTeamBId(randomTeam.getId());
        }

        match.setMatchNo("PK" + System.currentTimeMillis());
        match.setStatus(0);
        match.setCurrentRound(0);
        match.setCreatedAt(new Date());
        if (match.getMatchType() == null) match.setMatchType(1);
        if (match.getTotalRounds() == null) match.setTotalRounds(10);

        // 如果没有指定试卷，自动选择一份已发布的试卷
        if (match.getPaperId() == null) {
            LambdaQueryWrapper<ExamPaper> paperWrapper = new LambdaQueryWrapper<>();
            paperWrapper.eq(ExamPaper::getStatus, 1).last("ORDER BY RAND() LIMIT 1");
            ExamPaper paper = examPaperMapper.selectOne(paperWrapper);
            if (paper != null) {
                match.setPaperId(paper.getId());
                // 以试卷题目数作为总轮数
                long questionCount = examPaperQuestionMapper.selectCount(
                    new LambdaQueryWrapper<ExamPaperQuestion>().eq(ExamPaperQuestion::getPaperId, paper.getId())
                );
                match.setTotalRounds((int) questionCount);
                match.setCurrentRound(1);
            }
        }

        matchMapper.insert(match);

        // 如果是挑战指定队伍，发送PK通知
        if (match.getTeamBId() != null) {
            PkTeam teamA = teamMapper.selectById(match.getTeamAId());
            PkTeam teamB = teamMapper.selectById(match.getTeamBId());
            if (teamA != null && teamB != null) {
                String captainQuery = "选择了";
                // 通知B队队长
                LambdaQueryWrapper<PkTeamMember> cm = new LambdaQueryWrapper<PkTeamMember>()
                    .eq(PkTeamMember::getTeamId, match.getTeamBId())
                    .eq(PkTeamMember::getRole, 1)
                    .eq(PkTeamMember::getStatus, 1);
                List<PkTeamMember> captains = teamMemberMapper.selectList(cm);
                for (PkTeamMember c : captains) {
                    sysMessageService.sendMessage(c.getUserId(), 6,
                        "PK挑战通知：" + teamA.getTeamName() + "已向您发起PK挑战",
                        "队伍「" + teamA.getTeamName() + "」已向您的队伍「" + teamB.getTeamName() + "」发起PK挑战，请及时参加！",
                        match.getId(), "/pk/match/" + match.getId());
                }
            }
        }

        return match;
    }

    @Override
    public PkMatch getMatchDetail(Long matchId) {
        PkMatch match = matchMapper.selectById(matchId);
        if (match != null) {
            // 加载队伍名称
            PkTeam teamA = teamMapper.selectById(match.getTeamAId());
            PkTeam teamB = teamMapper.selectById(match.getTeamBId());
            if (teamA != null) match.setTeamAName(teamA.getTeamName());
            if (teamB != null) match.setTeamBName(teamB.getTeamName());
            // 加载获胜队伍名称
            if (match.getWinnerTeamId() != null) {
                PkTeam winner = teamMapper.selectById(match.getWinnerTeamId());
                if (winner != null) match.setWinnerTeamName(winner.getTeamName());
            }
        }
        return match;
    }

    @Override
    public List<PkMatchRecord> getMatchRecords(Long matchId) {
        LambdaQueryWrapper<PkMatchRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PkMatchRecord::getMatchId, matchId).orderByAsc(PkMatchRecord::getCreatedAt);
        List<PkMatchRecord> records = matchRecordMapper.selectList(wrapper);

        // 填充用户名
        if (!records.isEmpty()) {
            List<Long> userIds = records.stream().map(PkMatchRecord::getUserId).distinct().toList();
            List<User> users = userMapper.selectBatchIds(userIds);
            java.util.Map<Long, User> userMap = new java.util.HashMap<>();
            for (User u : users) userMap.put(u.getId(), u);
            for (PkMatchRecord r : records) {
                User u = userMap.get(r.getUserId());
                if (u != null) r.setUsername(u.getUsername());
            }
        }
        return records;
    }

    @Override
    @Transactional
    public PkMatch endMatch(Long matchId, Long userId) {
        PkMatch match = matchMapper.selectById(matchId);
        if (match == null) throw new RuntimeException("对战不存在");
        if (match.getStatus() != 0) throw new RuntimeException("对战已结束");

        // 计算两队得分
        LambdaQueryWrapper<PkMatchRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(PkMatchRecord::getMatchId, matchId);

        int scoreA = matchRecordMapper.selectCount(
            new LambdaQueryWrapper<PkMatchRecord>()
                .eq(PkMatchRecord::getMatchId, matchId)
                .eq(PkMatchRecord::getTeamId, match.getTeamAId())
                .eq(PkMatchRecord::getIsCorrect, 1)
        ).intValue();

        int scoreB = matchRecordMapper.selectCount(
            new LambdaQueryWrapper<PkMatchRecord>()
                .eq(PkMatchRecord::getMatchId, matchId)
                .eq(PkMatchRecord::getTeamId, match.getTeamBId())
                .eq(PkMatchRecord::getIsCorrect, 1)
        ).intValue();

        // 判定胜负
        match.setEndTime(new Date());
        if (scoreA > scoreB) {
            match.setWinnerTeamId(match.getTeamAId());
        } else if (scoreB > scoreA) {
            match.setWinnerTeamId(match.getTeamBId());
        }
        match.setStatus(1); // 已结束
        matchMapper.updateById(match);

        // 更新队伍战绩
        updateTeamRecord(match);

        return match;
    }

    private void updateTeamRecord(PkMatch match) {
        PkTeam teamA = teamMapper.selectById(match.getTeamAId());
        PkTeam teamB = teamMapper.selectById(match.getTeamBId());
        if (teamA == null || teamB == null) return;

        if (match.getWinnerTeamId() == null) {
            // 平局
            return;
        }

        if (match.getWinnerTeamId().equals(match.getTeamAId())) {
            teamA.setWinCount((teamA.getWinCount() == null ? 0 : teamA.getWinCount()) + 1);
            teamB.setLoseCount((teamB.getLoseCount() == null ? 0 : teamB.getLoseCount()) + 1);
        } else {
            teamB.setWinCount((teamB.getWinCount() == null ? 0 : teamB.getWinCount()) + 1);
            teamA.setLoseCount((teamA.getLoseCount() == null ? 0 : teamA.getLoseCount()) + 1);
        }

        teamMapper.updateById(teamA);
        teamMapper.updateById(teamB);
    }

    @Override
    @Transactional
    public PkMatchRecord submitAnswer(Long userId, PkMatchRecord record) {
        record.setUserId(userId);
        record.setCreatedAt(new Date());
        matchRecordMapper.insert(record);
        return record;
    }

    @Override
    public List<Leaderboard> getLeaderboard(Integer type, Integer period) {
        return leaderboardMapper.selectLeaderboard(type, period, 50);
    }

    @Override
    public PkTeam getMyTeam(Long userId) {
        LambdaQueryWrapper<PkTeamMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(PkTeamMember::getUserId, userId)
                .eq(PkTeamMember::getStatus, 1);
        PkTeamMember member = teamMemberMapper.selectOne(memberWrapper);
        if (member == null) return null;
        return teamMapper.selectById(member.getTeamId());
    }

    @Override
    public List<Leaderboard> getTopLeaderboard() {
        // 周榜Top10：period_type=1(周榜), leaderboard_type=1(个人总积分)
        return leaderboardMapper.selectLeaderboard(1, 1, 10);
    }

    @Override
    public void dismissTeam(Long teamId) {
        PkTeam team = new PkTeam();
        team.setId(teamId);
        team.setStatus(0);
        teamMapper.updateById(team);
    }

    @Override
    public List<PkTeamMember> listTeamMembers(Long teamId) {
        LambdaQueryWrapper<PkTeamMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PkTeamMember::getTeamId, teamId)
               .eq(PkTeamMember::getStatus, 1);
        List<PkTeamMember> members = teamMemberMapper.selectList(wrapper);

        // 填充用户信息
        if (!members.isEmpty()) {
            List<Long> userIds = members.stream().map(PkTeamMember::getUserId).distinct().toList();
            List<User> users = userMapper.selectBatchIds(userIds);
            java.util.Map<Long, User> userMap = new java.util.HashMap<>();
            for (User u : users) userMap.put(u.getId(), u);
            for (PkTeamMember m : members) {
                User u = userMap.get(m.getUserId());
                if (u != null) {
                    m.setUsername(u.getUsername());
                    m.setNickname(u.getNickname());
                }
            }
        }
        return members;
    }

    @Override
    public List<PkMatch> listTeamMatches(Long teamId) {
        LambdaQueryWrapper<PkMatch> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(PkMatch::getTeamAId, teamId).or().eq(PkMatch::getTeamBId, teamId))
               .orderByDesc(PkMatch::getCreatedAt);
        return matchMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public void updateTeam(Long teamId, PkTeam updates) {
        PkTeam team = teamMapper.selectById(teamId);
        if (team == null || team.getStatus() != 1) {
            throw new RuntimeException("队伍不存在或已解散");
        }
        if (StrUtil.isNotBlank(updates.getTeamName())) team.setTeamName(updates.getTeamName().trim());
        if (updates.getSlogan() != null) team.setSlogan(updates.getSlogan());
        if (updates.getMaxMembers() != null) {
            int max = updates.getMaxMembers();
            if (max < 2) throw new RuntimeException("最大人数不能少于2");
            if (max > 50) throw new RuntimeException("最大人数不能超过50");
            if (max < (team.getCurrentMembers() != null ? team.getCurrentMembers() : 0)) {
                throw new RuntimeException("最大人数不能小于当前成员数");
            }
            team.setMaxMembers(max);
        }
        team.setUpdatedAt(new Date());
        teamMapper.updateById(team);
    }

    @Override
    @Transactional
    public void removeTeamMember(Long teamId, Long userId) {
        LambdaQueryWrapper<PkTeamMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PkTeamMember::getTeamId, teamId)
               .eq(PkTeamMember::getUserId, userId)
               .eq(PkTeamMember::getStatus, 1)
               .last("LIMIT 1");
        PkTeamMember member = teamMemberMapper.selectOne(wrapper);
        if (member == null) {
            throw new RuntimeException("该成员不在队伍中");
        }
        if (member.getRole() == 1) {
            throw new RuntimeException("不能移除队长，请先转让队长");
        }
        // 标记为已退出
        member.setStatus(0);
        teamMemberMapper.updateById(member);

        // 更新队伍人数
        PkTeam team = teamMapper.selectById(teamId);
        if (team != null) {
            team.setCurrentMembers(Math.max(0, (team.getCurrentMembers() != null ? team.getCurrentMembers() : 0) - 1));
            team.setUpdatedAt(new Date());
            teamMapper.updateById(team);
        }
    }

    @Override
    public List<PkMatch> listAllMatches() {
        LambdaQueryWrapper<PkMatch> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(PkMatch::getCreatedAt);
        List<PkMatch> matches = matchMapper.selectList(wrapper);
        // 填充队名
        if (!matches.isEmpty()) {
            java.util.Set<Long> teamIds = new java.util.HashSet<>();
            for (PkMatch m : matches) {
                if (m.getTeamAId() != null) teamIds.add(m.getTeamAId());
                if (m.getTeamBId() != null) teamIds.add(m.getTeamBId());
            }
            if (!teamIds.isEmpty()) {
                List<PkTeam> teams = teamMapper.selectBatchIds(teamIds);
                java.util.Map<Long, String> nameMap = new java.util.HashMap<>();
                for (PkTeam t : teams) nameMap.put(t.getId(), t.getTeamName());
                for (PkMatch m : matches) {
                    m.setTeamAName(nameMap.get(m.getTeamAId()));
                    m.setTeamBName(nameMap.get(m.getTeamBId()));
                }
            }
        }
        return matches;
    }

    @Override
    @Transactional
    public void cancelMatch(Long matchId) {
        PkMatch match = matchMapper.selectById(matchId);
        if (match == null) throw new RuntimeException("对战不存在");
        if (match.getStatus() == 2) throw new RuntimeException("对战已结束");
        match.setStatus(2); // 标记为已结束/取消
        match.setEndTime(new Date());
        matchMapper.updateById(match);
    }
}
