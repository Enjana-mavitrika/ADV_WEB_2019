package org.coursecollector.esi;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.coursecollector.esi.model.Class;
import org.coursecollector.esi.model.ClassRepository;
import org.coursecollector.esi.model.Course;
import org.coursecollector.esi.model.CourseRepository;
import org.coursecollector.esi.model.Option;
import org.coursecollector.esi.model.OptionRepository;
import org.coursecollector.esi.model.Request;
import org.coursecollector.esi.model.RequestRepository;
import org.coursecollector.esi.model.Student;
import org.coursecollector.esi.model.StudentRepository;
import org.coursecollector.esi.model.Subject;
import org.coursecollector.esi.model.SubjectRepository;
import org.coursecollector.esi.model.Rate;
import org.coursecollector.esi.model.RateRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import java.util.Collection;
import org.coursecollector.esi.service.OcrService;
import org.coursecollector.esi.service.PdfService;

@Controller
public class MainController {

    @Inject
    StudentRepository studentRepo;

    @Inject
    ClassRepository classRepo;

    @Inject
    SubjectRepository subjectRepo;

    @Inject
    CourseRepository courseRepo;

    @Inject
    RequestRepository requestRepo;

    @Inject
    OptionRepository optionRepo;
    
    @Inject
    RateRepository rateRepo;
    
    @Inject
    OcrService ocrServ;
    
    @Inject
    PdfService pdfServ;

    private static final int MAX_NOTIFICATION_PER_STUDENT = 300;

    /**
     * 
     * @param model Object to send data to the view
     * @return
     */
    @RequestMapping("/")
    public String pageAccueil(Model model) {
        // load data test if necessary 
        if (((Collection<Class>)classRepo.findAll()).size() == 0) {
            return "redirect:/test-data";
        }
        // otherwise redirect to the default student's list of classes
        return "redirect:/class";
    }

    /**
     * 
     * @param model Object to send data to the view
     * @return HTML page class
     */
    @RequestMapping("/class")
    public String listClasses(Model model) {
        Student student = studentRepo.findById(TestController.defaultStudentId).get();
        List<Class> listClass = student.getClasses();
        model.addAttribute("class", listClass);
        // get notifications that concern the student
        model.addAttribute("notifications",
                MainController.listNotifications(studentRepo, TestController.defaultStudentId));
        return "class";
    }

    /**
     * 
     * @param model     Object to send data to the view
     * @param subjectId Unique identifier of the subject
     * @return HTML page course
     */
    @RequestMapping("/course")
    public String listCourses(Model model, @RequestParam Long subjectId) {
        Subject subject = subjectRepo.findById(subjectId).get();
        Iterable<Request> requests = requestRepo.findAll();
        Course course = new Course();
        // bound the subject to the course
        course.setSubjectId(subject.getId());
        Request boundedReq = new Request();
        boundedReq.setSubjectId(subject.getId());
        model.addAttribute("subject", subject);
        model.addAttribute("actreq", requests);
        model.addAttribute("course", course);
        model.addAttribute("request", boundedReq);
        model.addAttribute("studentId", TestController.defaultStudentId);
        // get notifications that concern the student
        model.addAttribute("notifications",
                MainController.listNotifications(studentRepo, TestController.defaultStudentId));
        return "course";
    }
    
    /**
     * Rate a course
     * @author Solofo R.
     * @param @RequestParam int like
     * @param @RequestParam Long studentId
     * @param @RequestParam Long courseId
     * @param @RequestParam Long subjectId
     * @return String
     */
    @RequestMapping("/rateCourse")
    public String rateCourse(@RequestParam int like, @RequestParam Long studentId, @RequestParam Long courseId, @RequestParam Long subjectId) {
        boolean dislike = (like == 0) ? true : false;
        Course course = courseRepo.findById(courseId).get();
        Student student = studentRepo.findById(studentId).get();
        Rate rate = null;
        // check if the student has already rate the course
        for (int i = 0; i < course.getRates().size(); i++) {
            if (course.getRates().get(i).getStudent() != null) {
                if (course.getRates().get(i).getStudent().getId() == studentId) {
                    rate = course.getRates().get(i);
                    // update rate
                    rate.setDislike(dislike);
                }
            }
        }
        // if not create new Rate
        if (rate == null) {
            rate = new Rate(dislike, course, student);
            // add new rate to the course
            course.getRates().add(rate);
        }
        // save or update new rate in DB
        rateRepo.save(rate);
        // update course
        courseRepo.save(course);
        
        return  "redirect:/course?subjectId=" + subjectId + "#course-" + courseId;
    }

    /**
     * Get list of class where the student is subscribed and get the option that
     * concern the student then go through all the subject in this option and
     * finally get all request of each of those subject.
     * 
     * @author Solofo RABONARIJAONA
     * 
     * @param studentRepo
     * @param studentId
     * @return String[][] - all the notification that concern the student
     */
    public static String[][] listNotifications(StudentRepository studentRepo, Long studentId) {
        int numberNotificationInfos = 4;
        int notifCounter = 0;
        String[][] notifications = new String[MAX_NOTIFICATION_PER_STUDENT][numberNotificationInfos];
        Student student = studentRepo.findById(studentId).get();
        List<Class> studentClasses = student.getClasses();
        for (int i = 0; i < studentClasses.size(); i++) {
            List<Option> options = studentClasses.get(i).getOptions();
            for (int j = 0; j < options.size(); j++) {
                //if (options.get(j).getName() == student.getOption()) {
                    List<Subject> subjects = options.get(j).getSubjects();
                    for (int k = 0; k < subjects.size(); k++) {
                        List<Request> requests = subjects.get(k).getRequests();
                        for (int l = 0; l < requests.size(); l++) {
                            notifications[notifCounter][0] = "" + subjects.get(k).getId();
                            notifications[notifCounter][1] = studentClasses.get(i).getName() + " "
                                    + studentClasses.get(i).getLevel() + " " + options.get(j).getName();
                            notifications[notifCounter][2] = subjects.get(k).getName();
                            notifications[notifCounter][3] = requests.get(l).getDateCourse().toString();
                            notifCounter++;
                        }
                    }
                //}
            }
        }
        // return extracted notifications
        return Arrays.copyOfRange(notifications, 0, notifCounter);
    }

    /**
     * 
     * @param model    Object to send data to the view
     * @param courseId Unique identifier of the course
     * @return HTML page content
     */
    @RequestMapping("/content")
    public String listCoursesContent(Model model, @RequestParam Long courseId) {
        Course course = courseRepo.findById(courseId).get();
        model.addAttribute("course", course);
        // get notifications that concern the student
        model.addAttribute("notifications",
                MainController.listNotifications(studentRepo, TestController.defaultStudentId));
        return "content";
    }

    /**
     * 
     * @param model Object to send data to the view
     * @return HTML page login
     */
    @RequestMapping("/login")
    public String login(Model model) {
        return "login";
    }

    /**
     * 
     * @param model      Object to send data to the view
     * @param newRequest
     * @return Redirection to the HTML page request-success
     */
    @PostMapping("/addRequest")
    public String addRequest(Model model, @ModelAttribute Request newRequest) {
        // save the new request in DB
        requestRepo.save(newRequest);
        // get the subject
        Subject subject = subjectRepo.findById(newRequest.getSubjectId()).get();
        // add new request to the current subject
        subject.getRequests().add(newRequest);
        // update subject
        subjectRepo.save(subject);
        return "redirect:/request-success?subjectId=" + subject.getId();
    }

    /**
     * 
     * @param model Object to send data to the view
     * @return HTML page request-success
     */
    @RequestMapping("/request-success")
    public String confirmation(Model model, @RequestParam Long subjectId) {
        // get notifications that concern the student
        model.addAttribute("notifications",
                MainController.listNotifications(studentRepo, TestController.defaultStudentId));
        // send the subjectId to the viewModel
        model.addAttribute("subjectId", subjectId);
        return "request-success";
    }

    /**
     * 
     * @param model Object to send data to the view
     * @return HTML page setting
     */
    @RequestMapping("/setting")
    public String setting(Model model) {
        // get notifications that concern the student
        model.addAttribute("notifications",
                MainController.listNotifications(studentRepo, TestController.defaultStudentId));
        return "setting";
    }

    /**
     * 
     * @param model  Object to send data to the view
     * @param course Unique identifier of the course
     * @return Function multiFileUpload
     */
    @RequestMapping(value = "/publish-success", method = RequestMethod.POST)
    public String publishSuccess(Model model, @ModelAttribute("course") Course course) {
        // get notifications that concern the student
        model.addAttribute("notifications",
                MainController.listNotifications(studentRepo, TestController.defaultStudentId));
        return this.multiFileUpload(model, course);
    }

    /**
     * 
     * @param model  Object to send data to the view
     * @param course Unique identifier of the course
     * @return HTML page publish-success
     */
    private String multiFileUpload(Model model, Course course) {
        // get subject of this course 
        Subject subject = subjectRepo.findById(course.getSubjectId()).get();
        String uploadRootPath = "./src/main/resources/static/img/courses/" + subject.getName() + "/";
        String pdfUploadRootPath = "./src/main/resources/static/pdf/courses/" + subject.getName() + "/";
        String imgFolderPath = "img/courses/" + subject.getName() + "/";
        String pdfFolderPath = "pdf/courses/" + subject.getName() + "/";
        String pdfFileName = course.getName() + "_" + course.getDateCourse();
        File uploadRootDir = new File(uploadRootPath);
        File pdfUploadRootDir = new File(pdfUploadRootPath);
        if (!(uploadRootDir.exists() && pdfUploadRootDir.exists())) {
            uploadRootDir.mkdirs();
            pdfUploadRootDir.mkdirs();
        }
        MultipartFile[] fileDatas = course.getFiles();
        List<String> linkedFiles = new ArrayList<>();
        List<String> failedFiles = new ArrayList<>();
        List<Integer> pages = new ArrayList<>();
        int i = 1;
        String text = "";
        
        for (MultipartFile fileData : fileDatas) {
            String name = course.getDateCourse() + "_" + fileData.getOriginalFilename();
            if (name != null && name.length() > 0) {
                try {
                    // convert img into text using OCR service
                    text += "\n\n\n" + ocrServ.multipartFileToString(fileData);
                    
                    File serverFile = new File(uploadRootDir.getAbsolutePath() + File.separator + name);
                    BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(serverFile));
                    stream.write(fileData.getBytes());
                    stream.close();
                    linkedFiles.add(imgFolderPath + name);
                    pages.add(i);
                    i++;
                } catch (Exception e) {
                    System.out.println("Error Write file: " + name);
                    failedFiles.add(name);
                }
            }
        }
        try {
            // convert extracted text by OCR service to PDF file
            pdfServ.textToPdf(text, pdfUploadRootPath, pdfFileName);
        } catch (Exception e) {
            System.out.println(e);
        }
                    
        // add student, links and pages to the course
        course.setStudent(studentRepo.findById(TestController.defaultStudentId).get());
        course.setLinks(linkedFiles);
        course.setPages(pages);
        course.setPdfLink(pdfFolderPath + pdfFileName + ".pdf");
        // save new course in DB
        courseRepo.save(course);
        // add course to the correspondant subject
        Subject correspondantSubject = subjectRepo.findById(course.getSubjectId()).get();
        correspondantSubject.getCourses().add(course);
        // update subject
        subjectRepo.save(correspondantSubject);
        model.addAttribute("course", course);
        model.addAttribute("failedFiles", failedFiles);
        return "publish-success";
    }

}
