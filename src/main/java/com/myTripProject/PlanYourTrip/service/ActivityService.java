package com.myTripProject.PlanYourTrip.service;

import com.myTripProject.PlanYourTrip.dto.ActivityRequest;
import com.myTripProject.PlanYourTrip.dto.ActivityResponse;
import com.myTripProject.PlanYourTrip.entity.Activity;
import com.myTripProject.PlanYourTrip.entity.Destination;
import com.myTripProject.PlanYourTrip.exception.BadRequestException;
import com.myTripProject.PlanYourTrip.exception.ResourceNotFoundException;
import com.myTripProject.PlanYourTrip.repository.ActivityRepository;
import com.myTripProject.PlanYourTrip.repository.DestinationRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ActivityService {
    private final ActivityRepository activityRepository;
    private final DestinationRepository destinationRepository;
    ActivityService(ActivityRepository activityRepository, DestinationRepository destinationRepository) {
        this.activityRepository = activityRepository;
        this.destinationRepository = destinationRepository;
    }
    public ActivityResponse createActivity(Long destinationId, ActivityRequest request) {
        Destination destination = destinationRepository.findById(destinationId).orElseThrow(() -> new ResourceNotFoundException("No destination with requested id exists"));
        if (request.getEndTime().isBefore(request.getStartTime())) {
            throw new BadRequestException(
                    "Activity end time must be after start time"
            );
        }
        if (request.getDate().isBefore(destination.getStartDate())
                || request.getDate().isAfter(destination.getEndDate())) {
            throw new BadRequestException(
                    "Activity date must fall within destination dates"
            );
        }
        Activity activity = new Activity();
        activity.setDestination(destination);
        activity.setCategory(request.getCategory());
        activity.setCost(request.getCost());
        activity.setTitle(request.getTitle());
        activity.setDate(request.getDate());
        activity.setStartTime(request.getStartTime());
        activity.setEndTime(request.getEndTime());
        activityRepository.save(activity);
        return createActivityResponse(activity);
    }
    public Activity createActivity2(Destination destination, ActivityRequest request) {
        if(!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BadRequestException("Start time cannot be after End time");
        }
        if(request.getDate().isBefore(destination.getStartDate()) || request.getDate().isAfter(destination.getEndDate())) {
            throw new BadRequestException("Date does not lie within Destination dates");
        }
        Activity activity = new Activity();
        activity.setDestination(destination);
        activity.setCategory(request.getCategory());
        activity.setCost(request.getCost());
        activity.setTitle(request.getTitle());
        activity.setDate(request.getDate());
        activity.setStartTime(request.getStartTime());
        activity.setEndTime(request.getEndTime());
        return activity;
    }
    public List<ActivityResponse> getActivities(Long destinationId) {
        destinationRepository.findById(destinationId).orElseThrow(() -> new ResourceNotFoundException("Destination does not exist"));
        List<Activity> activities = activityRepository.findByDestinationId(destinationId);
        List<ActivityResponse> activityResponses = new ArrayList<>();
        for(Activity activity : activities) {
            activityResponses.add(createActivityResponse(activity));
        }
        return activityResponses;
    }
    public ActivityResponse getActivityById(Long activityId) {
        Activity activity = activityRepository.findById(activityId).orElseThrow(() -> new ResourceNotFoundException("No activity with this id exists"));
        return createActivityResponse(activity);
    }
    public ActivityResponse createActivityResponse(Activity activity) {
        ActivityResponse activityResponse = new ActivityResponse();
        activityResponse.setId(activity.getId());
        activityResponse.setStartTime(activity.getStartTime());
        activityResponse.setEndTime(activity.getEndTime());
        activityResponse.setTitle(activity.getTitle());
        activityResponse.setDate(activity.getDate());
        activityResponse.setCost(activity.getCost());
        activityResponse.setCategory(activity.getCategory());
        return activityResponse;
    }
    public void deleteActivityById(Long activityId) {
        Activity activity = activityRepository.findById(activityId).orElseThrow(() -> new ResourceNotFoundException("No such activity exists"));
        activityRepository.delete(activity);
    }
}