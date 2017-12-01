package com.swiggy.order.stateslamonitor.service;

import java.util.Calendar;
import java.util.Date;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.swiggy.order.stateslamonitor.entity.OrderStateHisotry;
import com.swiggy.order.stateslamonitor.entity.StateSla;
import com.swiggy.order.stateslamonitor.rabbitmq.IOrderStateSlaAlertService;
import com.swiggy.order.stateslamonitor.repository.OrderStateHistoryRepository;
import com.swiggy.order.stateslamonitor.repository.StateSlaRepository;

public class MonitorOrderStateSlaService implements IMonitorOrderStateSlaService {

	private static Logger logger = LoggerFactory.getLogger(MonitorOrderStateSlaService.class);

	@Autowired
	private StateSlaRepository stateSlaRepository;

	@Autowired
	private OrderStateHistoryRepository orderStateHistoryRepository;
	
	@Autowired
	private IOrderStateSlaAlertService orderStateSlaAlertService;

	public void monitorOrderStateSla() {
		Set<OrderStateHisotry> orderStateHistories = orderStateHistoryRepository.findAllUnMonitoringOrderStateHistories();
		for(OrderStateHisotry orderStateHistory : orderStateHistories){
			checkOrderStateSlaBreach(orderStateHistory);
		}
	}

	private void checkOrderStateSlaBreach(OrderStateHisotry orderStateHistory) {
		// we can have cache here over this repository result
		StateSla stateSla = stateSlaRepository.findByFinalState(orderStateHistory.getState());
		if(isSlaAcceptable(orderStateHistory.getStateUpdatedTime(), stateSla.getAcceptableValueInMins())){
			logger.info("SLA in not acceptable for orderStateHistory : {}", orderStateHistory);
			orderStateSlaAlertService.alertOrderBreachSla(orderStateHistory.getOrderCode(),orderStateHistory.getState());
		} else{
			logger.info("SLA in acceptable for orderStateHistory : {}", orderStateHistory);
		}
		orderStateHistory.setMonitoringProcessed(true);
		orderStateHistoryRepository.save(orderStateHistory);
		return;
	}

	private boolean isSlaAcceptable(Date stateChangeTime, Integer accetableSlaInMin) {
		return (Calendar.getInstance().getTimeInMillis() - stateChangeTime.getTime())/1000 >=  accetableSlaInMin*60l;
	}

}
