package com.example.ipldashboard.data;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import com.example.ipldashboard.model.Team;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;


@Component
public class JobCompletionNotificationListener extends JobExecutionListenerSupport {

    private static final Logger log = LoggerFactory.getLogger(JobCompletionNotificationListener.class);

    private final EntityManager eManager;

    @Autowired
    public JobCompletionNotificationListener(EntityManager eManager) {
        this.eManager = eManager;
    }

    @Override
    @Transactional
    public void afterJob(JobExecution jobExecution) {
        if(jobExecution.getStatus() == BatchStatus.COMPLETED) {
            log.info("!!! JOB FINISHED! Time to verify the results");

            Map<String , Team> teamData = new HashMap<>() ;

            eManager.createQuery("select m.team1, count(*) from Match m group by m.team1 ", Object[].class)
                    .getResultList().stream()
                    .map(e -> new Team((String)e[0], (Long)e[1]))
                    .forEach(team -> teamData.put(team.getTeamName(), team));

            eManager.createQuery("select m.team2 , count(*)  from Match m group by m.team2  ", Object[].class)
                    .getResultList()
                    .forEach(e -> {
                        Team team = teamData.get((String) e[0]);
                        team.setTotalMatches(team.getTotalMatches() + (long) e[1]);
                    });
            eManager.createQuery("select m.matchWinner , count(*) from Match m group by m.matchWinner", Object[].class)
                    .getResultList()
                    .forEach(e-> {
                        Team team  = teamData.get((String) e[0]) ;
                        if (team != null) team.setTotalWins((long)e[1]);
                    });
//        System.out.println("Team Daata "+teamData);
            teamData.values().forEach(
                    eManager::persist
            );
            teamData.values().forEach(System.out::println);




        }
    }
}