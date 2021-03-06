#import "PictureUploadHandler.h"
#import "ASIFormDataRequest.h"
#import "settings.h"

static CurrentOrderDetailViewController *currentOrderViewController = nil;

@implementation PictureUploadHandler
{
    UIImage *_img;
    ASIFormDataRequest *_request;
    PictureUploadDelegate *_target;
}

+ (void)setCurrentOrderViewController:(CurrentOrderDetailViewController*)controller
{
    currentOrderViewController = controller;
}

+ (NSArray*)getUploadingPictures
{
    return [PictureUploadHandler uploadingPictures];
}

+ (NSMutableArray*)uploadingPictures
{
    static NSMutableArray* uploadingPictures = nil;

    if(!uploadingPictures)
        uploadingPictures = [[NSMutableArray alloc] init];

    return uploadingPictures;
}

- (id)initWithImage:(UIImage*)img target:(PictureUploadDelegate*)target
{
    self = [super init];
    if(self)
    {
        _img = img;
        _target = target;
    }
    return self;
}

- (void)startPictureUpload
{
    if([[PictureUploadHandler uploadingPictures] containsObject:_img])
        return;

    [[PictureUploadHandler uploadingPictures] addObject:_img];

    [currentOrderViewController uploadingPicturesChanged];

    _request = [ASIFormDataRequest requestWithURL:[NSURL URLWithString:[WEB_SERVICE_BASE_URI stringByAppendingString:@"pictures/"]]];
    [_request setRequestMethod:@"PUT"];
    [_request setData:UIImageJPEGRepresentation(_img, 0.95) withFileName:@"doesntmatter.jpg" andContentType:@"image/jpeg" forKey:@"picture"];
    [_request setDelegate:self];
    [_request startAsynchronous];
}

#pragma mark - ASIFormDataRequest delegate

- (void)requestFinished:(ASIHTTPRequest*)request
{
    [self onFinish];

    NSString *error = nil;

    if([request responseStatusCode] < 200 || [request responseStatusCode] > 299)
        error = [NSString stringWithFormat:@"Response code: %d", [request responseStatusCode]];

    [_target pictureUploadFinished:_img error:error];
}

- (void)requestFailed:(ASIHTTPRequest*)request
{
    [self onFinish];

    NSError *error = [request error];

    [_target pictureUploadFinished:_img error:[error localizedDescription]];
}

- (void)onFinish
{
    [[PictureUploadHandler uploadingPictures] removeObject:_img];

    [currentOrderViewController uploadingPicturesChanged];
}

@end
