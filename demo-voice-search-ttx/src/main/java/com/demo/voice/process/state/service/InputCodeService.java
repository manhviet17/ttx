package com.demo.voice.process.state.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

import com.demo.voice.nlu.analyzer.dto.NluIntentDTO;
import com.demo.voice.process.cache.CacheManager;
import com.demo.voice.process.dto.Response;
import com.demo.voice.process.service.ADService;
import com.demo.voice.process.service.FuzzySearch;
import com.demo.voice.process.service.StatusFSM;
//@Configuration
//@ComponentScan(basePackages = { "com.demo.voice.process.state.service.*" })
//@PropertySources(@PropertySource("classpath:voice-search-ttx.properties"))

public class InputCodeService implements StateService {
//	@Value("${expireTimeCode}")
	private long expireTimeCode=120000;
	
	@Override
	public void doWork(StatusFSM statusFSM, NluIntentDTO intent, Response response, CacheManager cacheManager,
			ADService adService, FuzzySearch fuzzySearch, String uid) {
		// TODO Auto-generated method stub
		statusFSM.fireEvent(intent.getIntent());
		org.apache.commons.scxml.model.State state = statusFSM.getCurrentState();
		String code = intent.getCode();
		cacheManager.getCacheByUid(uid).setCode(code);
		String userId = cacheManager.getCacheByUid(uid).getUserId();
		String d = ((org.apache.commons.scxml.model.Data) state.getDatamodel().getData().get(0)).getExpr();
		response.setDialogMessage(d);
		switch (state.getId()) {
		case "inputResetCode":
			code = intent.getCode();
			if ((System.currentTimeMillis() - cacheManager.getCacheByUid(uid).getTimeCode()) >= expireTimeCode) {
				cacheManager.getCacheByUid(uid).setTimeCode(0);
				d = ((org.apache.commons.scxml.model.Data) state.getDatamodel().getData().get(1)).getExpr();
				statusFSM.fireEvent("backInputResetCode");
				String d2 = ((org.apache.commons.scxml.model.Data) state.getDatamodel().getData().get(0)).getExpr();
				response.setDialogMessage(d + " " + d2);
			} else {
				if (adService.veriryCode(code, userId)) {
					statusFSM.fireEvent("correct-code");
					state = statusFSM.getCurrentState();
					d = ((org.apache.commons.scxml.model.Data) state.getDatamodel().getData().get(0)).getExpr();
					adService.resetPassword(userId);
					response.setDialogMessage(d);
					statusFSM.fireEvent("login-confirm");
				} else {
					statusFSM.fireEvent("incorrect-code");
					state = statusFSM.getCurrentState();
					d = ((org.apache.commons.scxml.model.Data) state.getDatamodel().getData().get(0)).getExpr();
					response.setCode(code);
					response.setDialogMessage(d);
				}
			}
			break;
		case "inputUnlockCode":
			code = intent.getCode();
			if (adService.veriryCode(code, userId)) {
				statusFSM.fireEvent("correct-code");
				state = statusFSM.getCurrentState();
				d = ((org.apache.commons.scxml.model.Data) state.getDatamodel().getData().get(0)).getExpr();
				adService.unlockUser(userId);
				response.setDialogMessage(d);
				statusFSM.fireEvent("login-confirm");
			} else {
				statusFSM.fireEvent("incorrect-code");
				state = statusFSM.getCurrentState();
				d = ((org.apache.commons.scxml.model.Data) state.getDatamodel().getData().get(0)).getExpr();
				response.setCode(code);
				response.setDialogMessage(d);
			}
			break;
		}

	}

}
